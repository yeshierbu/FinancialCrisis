package com.erbu.financialcrisis.messaging;

import com.erbu.financialcrisis.domain.enums.ApprovalStep;
import java.time.LocalDateTime;


/**
 * 定义消息内容
 *
 * <p>队列只传递业务 ID 和流程控制字段，不传递身份证、OCR 全文等敏感数据；
 * 消费端收到消息后，会根据申请 ID 重新读取业务数据并执行指定审批步骤。</p>
 *
 * @param eventId 消息事件 ID，用于消费幂等和防止 RabbitMQ 重复投递导致重复审批：这笔申请中的哪一个审批步骤事件
 * @param applicationId 贷款申请 ID，用于定位要处理的申请记录：哪一笔贷款申请
 * @param applicationNo 贷款申请编号，用于日志、排查和人工识别
 * @param step 表示当亲啊消息应该执行哪个审批步骤
 * @param retryCount 当前消息已经重试的次数，用于判断是否超过最大重试上限
 * @param createdAt 原始消息创建时间，用于追踪消息生命周期
 */
public record ApprovalStartMessage(String eventId, Long applicationId, String applicationNo,
                                   ApprovalStep step, int retryCount, LocalDateTime createdAt) {
    /**
     * <p>如果滚动升级期间旧生产者没有写入 {@code step} 字段，则默认从资料接收步骤开始。</p>
     */
    public ApprovalStartMessage {
        if (step == null) step = ApprovalStep.DOCUMENT_INTAKE;
    }

    /**
     * 创建下一次重试使用的消息副本。
     * <p>重试时保持同一个 {@code eventId}、申请信息和审批步骤，只递增重试次数，
     * 便于消费日志继续按同一事件做幂等控制。</p>
     */
    public ApprovalStartMessage nextRetry() {
        return new ApprovalStartMessage(eventId, applicationId, applicationNo, step, retryCount + 1, createdAt);
    }
}
