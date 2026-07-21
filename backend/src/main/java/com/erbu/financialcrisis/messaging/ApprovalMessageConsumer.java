package com.erbu.financialcrisis.messaging;

import com.erbu.financialcrisis.config.ApprovalRabbitConfig;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.service.ApprovalConsumeService;
import com.erbu.financialcrisis.service.ApprovalTaskService;
import com.erbu.financialcrisis.store.ApprovalStore;
import com.erbu.financialcrisis.mapper.ApprovalExecutionLockMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.Set;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;

/** 有界并发的审批消费者；业务终态和 eventId 双重防重。 */
@Component
@ConditionalOnProperty(name = "approval.messaging.enabled", havingValue = "true")
public class ApprovalMessageConsumer {
    private static final Set<ApplicationStatus> TERMINAL = EnumSet.of(
            ApplicationStatus.APPROVED, ApplicationStatus.REJECTED,
            ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.ARCHIVED);
    private final AgentOrchestrationService orchestrationService;
    private final ApprovalConsumeService consumeService;
    private final ApprovalStore store;
    private final RabbitTemplate rabbitTemplate;
    private final ApprovalExecutionLockMapper lockMapper;
    private final ApprovalTaskService taskService;
    private final int maxRetries;
    private final String consumerName;

    public ApprovalMessageConsumer(AgentOrchestrationService orchestrationService,
                                   ApprovalConsumeService consumeService, ApprovalStore store,
                                   RabbitTemplate rabbitTemplate, ApprovalExecutionLockMapper lockMapper,
                                   ApprovalTaskService taskService,
                                   @Value("${approval.messaging.max-retries:3}") int maxRetries) {
        this.orchestrationService = orchestrationService;
        this.consumeService = consumeService;
        this.store = store;
        this.rabbitTemplate = rabbitTemplate;
        this.lockMapper = lockMapper;
        this.taskService = taskService;
        this.maxRetries = maxRetries;
        this.consumerName = resolveConsumerName();
    }

    @RabbitListener(queues = ApprovalRabbitConfig.APPROVAL_QUEUE)
    public void consume(ApprovalStartMessage payload, Message raw, Channel channel) throws IOException {
        long tag = raw.getMessageProperties().getDeliveryTag();
        String claimToken = consumeService.tryStart(payload, consumerName);
        if (claimToken == null) {
            channel.basicAck(tag, false);
            return;
        }
        if (!acquireApplicationLock(payload)) {
            consumeService.fail(payload, claimToken, "APPLICATION_BUSY");
            // 锁冲突不代表审批失败，延迟后继续等待，不能因此把仍在运行的申请转人工。
            rabbitTemplate.convertAndSend(ApprovalRabbitConfig.RETRY_EXCHANGE,
                    ApprovalRabbitConfig.RETRY_ROUTING_KEY, payload.nextRetry());
            channel.basicAck(tag, false);
            return;
        }
        try {
            LoanApplication application = store.getApplicationOrThrow(payload.applicationId());
            if (!TERMINAL.contains(application.getStatus())) {
                var nextStep = orchestrationService.executeStep(payload.applicationId(), payload.step());
                if (nextStep != null) taskService.submitStep(payload.applicationId(), nextStep, payload.eventId());
            }
            consumeService.complete(payload.eventId(), claimToken);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            consumeService.fail(payload, claimToken, ex.getMessage());
            try {
                retryOrMoveToManual(payload, ex.getMessage());
                channel.basicAck(tag, false);
            } catch (Exception publishFailure) {
                // 重试消息都未能发出时保留原消息，避免任务丢失。
                channel.basicNack(tag, false, true);
            }
        } finally {
            lockMapper.delete(payload.applicationId(), payload.eventId());
        }
    }

    private boolean acquireApplicationLock(ApprovalStartMessage payload) {
        LocalDateTime now = LocalDateTime.now();
        lockMapper.deleteExpired(payload.applicationId(), now);
        try {
            return lockMapper.insert(payload.applicationId(), payload.eventId(), consumerName,
                    now.plusMinutes(10)) == 1;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    private void retryOrMoveToManual(ApprovalStartMessage payload, String error) {
        if (payload.retryCount() < maxRetries) {
            rabbitTemplate.convertAndSend(ApprovalRabbitConfig.RETRY_EXCHANGE,
                    ApprovalRabbitConfig.RETRY_ROUTING_KEY, payload.nextRetry());
        } else {
            // 达到上限后先落人工审核工单，再把原消息留在死信队列供管理员排查。
            orchestrationService.moveToManualReview(payload.applicationId(),
                    "异步审批超过最大重试次数：" + (error == null ? "未知异常" : error));
            rabbitTemplate.convertAndSend(ApprovalRabbitConfig.DEAD_EXCHANGE,
                    ApprovalRabbitConfig.DEAD_ROUTING_KEY, payload);
        }
    }

    private String resolveConsumerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "approval-consumer";
        }
    }
}
