package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.config.ApprovalRabbitConfig;
import com.erbu.financialcrisis.domain.entity.ApprovalOutbox;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.mapper.ApprovalOutboxMapper;
import com.erbu.financialcrisis.messaging.ApprovalStartMessage;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.service.ApprovalTaskService;
import com.erbu.financialcrisis.store.ApprovalStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import com.erbu.financialcrisis.domain.enums.ApprovalStep;
import org.springframework.dao.DuplicateKeyException;

/**
 * 业务事务中只写 MySQL Outbox，不直接依赖 RabbitMQ 当时可用。
 * 测试或明确关闭消息模式时使用同步回退。
 */
@Service
public class ApprovalTaskServiceImpl implements ApprovalTaskService {
    private final ApprovalStore store;
    private final ApprovalOutboxMapper outboxMapper;
    private final AgentOrchestrationService orchestrationService;
    private final ObjectMapper objectMapper;
    private final boolean messagingEnabled;

    public ApprovalTaskServiceImpl(ApprovalStore store, ApprovalOutboxMapper outboxMapper,
                                   AgentOrchestrationService orchestrationService, ObjectMapper objectMapper,
                                   @Value("${approval.messaging.enabled:true}") boolean messagingEnabled) {
        this.store = store;
        this.outboxMapper = outboxMapper;
        this.orchestrationService = orchestrationService;
        this.objectMapper = objectMapper;
        this.messagingEnabled = messagingEnabled;
    }

    /**
     * 首个审批步骤
     * @param applicationId
     * @return
     */
    @Override
    public String submit(Long applicationId) {
        return submitStep(applicationId, ApprovalStep.DOCUMENT_INTAKE);
    }

    @Override
    public String submitStep(Long applicationId, ApprovalStep step) {
        if (!messagingEnabled) {
            ApprovalStep current = step;
            while (current != null) current = orchestrationService.executeStep(applicationId, current);
            return "SYNC-" + applicationId;
        }
        return persistStep(applicationId, step, UUID.randomUUID().toString());
    }

    /**
     * 为首次审批步骤生成一个唯一eventId
     * @param applicationId
     * @param step
     * @param parentEventId
     * @return
     */
    @Override
    public String submitStep(Long applicationId, ApprovalStep step, String parentEventId) {
        if (!messagingEnabled) return submitStep(applicationId, step);
        String eventId = UUID.nameUUIDFromBytes((parentEventId + ":" + step.name()).getBytes(StandardCharsets.UTF_8)).toString();
        return persistStep(applicationId, step, eventId);
    }

    private String persistStep(Long applicationId, ApprovalStep step, String eventId) {
        try {
            LoanApplication application = store.getApplicationOrThrow(applicationId);
            LocalDateTime now = LocalDateTime.now();
            ApprovalStartMessage message = new ApprovalStartMessage(
                    eventId, applicationId, application.getApplicationNo(), step, 0, now);
            ApprovalOutbox outbox = new ApprovalOutbox();
            outbox.setEventId(eventId);
            outbox.setApplicationId(applicationId);
            outbox.setEventType("APPROVAL_STEP_" + step.name());
            outbox.setRoutingKey(ApprovalRabbitConfig.APPROVAL_ROUTING_KEY);
            outbox.setPayloadJson(objectMapper.writeValueAsString(message));
            outbox.setPublishStatus("PENDING");
            outbox.setRetryCount(0);
            outbox.setCreatedAt(now);
            outbox.setUpdatedAt(now);
            try { outboxMapper.insert(outbox); } catch (DuplicateKeyException ignored) { }
            return eventId;
        } catch (Exception ex) {
            throw new IllegalStateException("创建审批 Outbox 任务失败", ex);
        }
    }
}
