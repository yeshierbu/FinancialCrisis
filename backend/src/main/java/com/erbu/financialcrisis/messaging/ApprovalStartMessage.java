package com.erbu.financialcrisis.messaging;

import com.erbu.financialcrisis.domain.enums.ApprovalStep;
import java.time.LocalDateTime;

/** 队列只传递业务 ID，不传递身份证、OCR 全文等敏感数据。 */
public record ApprovalStartMessage(String eventId, Long applicationId, String applicationNo,
                                   ApprovalStep step, int retryCount, LocalDateTime createdAt) {
    public ApprovalStartMessage {
        if (step == null) step = ApprovalStep.DOCUMENT_INTAKE; // rolling upgrade compatibility
    }
    public ApprovalStartMessage nextRetry() {
        return new ApprovalStartMessage(eventId, applicationId, applicationNo, step, retryCount + 1, createdAt);
    }
}
