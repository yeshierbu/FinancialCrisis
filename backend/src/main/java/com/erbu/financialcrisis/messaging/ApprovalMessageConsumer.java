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

    /**
     * 初始化审批消息消费者所需的业务服务、消息组件和并发控制组件。
     *
     * <p>消费者收到消息后，会先通过 {@code consumeService} 做 eventId 级别的消费防重，
     * 再通过 {@code lockMapper} 做 applicationId 级别的执行互斥，最后由
     * {@code orchestrationService} 推进审批步骤；失败时使用 {@code rabbitTemplate}
     * 投递重试消息或死信消息。</p>
     *
     * @param orchestrationService 审批步骤编排服务，负责执行当前步骤并返回下一步
     * @param consumeService 消息消费状态服务，负责消费声明、完成和失败记录
     * @param store 贷款申请存储服务，用于读取申请当前状态
     * @param rabbitTemplate RabbitMQ 发送模板，用于投递重试消息和死信消息
     * @param lockMapper 审批执行锁 Mapper，用于防止同一申请并发执行
     * @param taskService 审批任务服务，用于提交后续审批步骤
     * @param maxRetries 单条审批消息允许的最大重试次数
     */
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

    /**
     * 消费主审批队列中的审批步骤消息。
     *
     * <p>处理顺序为：先基于 eventId 抢占消费权，避免 RabbitMQ 重复投递导致重复决策；
     * 再基于 applicationId 获取执行锁，避免同一申请被多个消费者并发推进；最后执行当前
     * 审批步骤，并按结果确认消息、投递重试消息或转入人工审核。</p>
     *
     * @param payload 反序列化后的业务消息
     * @param raw RabbitMQ 原始消息，用于获取 deliveryTag 并进行手动 ack/nack
     * @param channel RabbitMQ Channel，执行ACK/NACK的通道
     * @throws IOException 手动确认 RabbitMQ 消息失败时抛出
     */
    @RabbitListener(queues = ApprovalRabbitConfig.APPROVAL_QUEUE)//消费者入口
    public void consume(ApprovalStartMessage payload, Message raw, Channel channel) throws IOException {
        long tag = raw.getMessageProperties().getDeliveryTag();   //获取RabbitMQ投递编号，用于channel的消息确认

        // 第一层防重：同一个 eventId 只允许一个消费者真正进入业务处理。
        String claimToken = consumeService.tryStart(payload, consumerName);
        if (claimToken == null) {
            channel.basicAck(tag, false);
            return;
        }

        // 第二层防并发：同一笔申请同时只能执行一个审批步骤。
        if (!acquireApplicationLock(payload)) {
            consumeService.fail(payload, claimToken, "APPLICATION_BUSY");
            // 锁冲突不代表审批失败，延迟后继续等待，不能因此把仍在运行的申请转人工。
            rabbitTemplate.convertAndSend(ApprovalRabbitConfig.RETRY_EXCHANGE,   //发送到重试队列
                    ApprovalRabbitConfig.RETRY_ROUTING_KEY, payload.nextRetry());  //payload.nextRetry()重试次数+1
            channel.basicAck(tag, false);//向主消息队列手动Ack并删除主队列原消息，防止主消息队列大量重复Ack
            return;   //当前消费者没有拿到锁，结束本地消费方法
        }
        try {
            LoanApplication application = store.getApplicationOrThrow(payload.applicationId());
            if (!TERMINAL.contains(application.getStatus())) {
                var nextStep = orchestrationService.executeStep(payload.applicationId(), payload.step());
                if (nextStep != null) taskService.submitStep(payload.applicationId(), nextStep, payload.eventId());
            }

            // 当前步骤已成功处理，记录完成并确认 RabbitMQ 删除原消息。
            consumeService.complete(payload.eventId(), claimToken);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            consumeService.fail(payload, claimToken, ex.getMessage());
            try {
                // 业务执行失败时，优先按重试次数投递延迟重试，超过上限再转人工和死信。
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
