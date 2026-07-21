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

    /**
     * 尝试声明当前消费者开始处理一条审批消息。
     *
     * <p>该方法通过 {@code eventId} 对消费日志做幂等控制：已完成的消息不会再次处理；
     * 失败、待重试或租约过期的处理中消息，可以被当前消费者重新抢占。返回的
     * {@code claimToken} 会作为后续完成或失败更新的凭证，避免其他消费者误改状态。</p>
     *
     * @param message 审批启动消息，包含 eventId、申请 ID、审批步骤和重试次数
     * @param consumerName 当前消费者名称，用于记录由哪个实例处理消息
     * @return 抢占成功时返回本次消费的 claimToken；抢占失败或消息已完成时返回 null
     */
    @Override
    @Transactional
    public String tryStart(ApprovalStartMessage message, String consumerName) {
        // 每次消费尝试生成独立 token，后续完成/失败时必须带上该 token 才能更新记录。
        String token = UUID.randomUUID().toString();
        LocalDateTime leaseUntil = LocalDateTime.now().plusSeconds(leaseSeconds);

        // 先按 eventId 查询历史消费记录，判断是否为重复投递或重试投递。
        ApprovalMessageConsumeLog existing = consumeMapper.selectByEventId(message.eventId());
        if (existing != null) {
            if ("COMPLETED".equals(existing.getConsumeStatus())) return null;
            // 只有失败、重试或租约过期的处理中消息，才能被当前消费者重新声明处理权。
            return consumeMapper.retryClaim(message.eventId(), consumerName, message.retryCount(), token, leaseUntil) == 1 ? token : null;
        }

        // 首次消费时插入 PROCESSING 记录，声明当前消费者正在处理该 eventId。
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
            // 并发消费者同时插入同一 eventId 时，唯一键冲突的一方放弃处理。
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
