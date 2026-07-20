package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.ApprovalMessageConsumeLog;
import com.erbu.financialcrisis.mapper.ApprovalMessageConsumeLogMapper;
import com.erbu.financialcrisis.mapper.ApprovalOutboxMapper;
import com.erbu.financialcrisis.messaging.ApprovalStartMessage;
import com.erbu.financialcrisis.service.ApprovalConsumeService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

/** eventId 唯一约束保证 RabbitMQ 重复投递不会生成重复最终决策。 */
@Service
public class ApprovalConsumeServiceImpl implements ApprovalConsumeService {
    private final ApprovalMessageConsumeLogMapper consumeMapper;
    private final ApprovalOutboxMapper outboxMapper;
    private final long leaseSeconds;

    public ApprovalConsumeServiceImpl(ApprovalMessageConsumeLogMapper consumeMapper,
                                      ApprovalOutboxMapper outboxMapper,
                                      @Value("${approval.messaging.consume-lease-seconds:120}") long leaseSeconds) {
        this.consumeMapper = consumeMapper;
        this.outboxMapper = outboxMapper;
        this.leaseSeconds = leaseSeconds;
    }

    @Override
    @Transactional
    public String tryStart(ApprovalStartMessage message, String consumerName) {
        String token = UUID.randomUUID().toString();
        LocalDateTime leaseUntil = LocalDateTime.now().plusSeconds(leaseSeconds);
        ApprovalMessageConsumeLog existing = consumeMapper.selectByEventId(message.eventId());
        if (existing != null) {
            if ("COMPLETED".equals(existing.getConsumeStatus())) return null;
            return consumeMapper.retryClaim(message.eventId(), consumerName, message.retryCount(), token, leaseUntil) == 1 ? token : null;
        }
        LocalDateTime now = LocalDateTime.now();
        ApprovalMessageConsumeLog log = new ApprovalMessageConsumeLog();
        log.setEventId(message.eventId());
        log.setApplicationId(message.applicationId());
        log.setConsumeStatus("PROCESSING");
        log.setConsumerName(consumerName);
        log.setRetryCount(message.retryCount());
        log.setClaimToken(token); log.setLeaseUntil(leaseUntil); log.setAttemptNo(1);
        log.setStartedAt(now);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        try {
            return consumeMapper.insertProcessing(log) == 1 ? token : null;
        } catch (DuplicateKeyException duplicate) {
            return null;
        }
    }

    @Override
    @Transactional
    public void complete(String eventId, String claimToken) {
        consumeMapper.markCompleted(eventId, claimToken);
        outboxMapper.markConsumed(eventId);
    }

    @Override
    @Transactional
    public void fail(ApprovalStartMessage message, String claimToken, String error) {
        consumeMapper.markFailed(message.eventId(), truncate(error), message.retryCount(), claimToken);
    }

    private String truncate(String value) {
        if (value == null) return "UNKNOWN";
        return value.length() <= 900 ? value : value.substring(0, 900);
    }
}
