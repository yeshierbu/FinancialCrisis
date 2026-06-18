package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工复核工单表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewTicket {

    private Long ticketId;
    private Long applicationId;
    private String ticketNo;
    private ReviewStatus reviewStatus;
    private String assignedTo;
    private String triggerReason;
    private String riskSummary;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
