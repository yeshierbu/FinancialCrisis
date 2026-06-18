package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待人工复核列表响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewPendingResponse {

    private Long applicationId;
    private String ticketNo;
    private RiskLevel riskLevel;
    private String productCode;
    private String applicantName;
    private ReviewStatus reviewStatus;
    private String assignedTo;
    private LocalDateTime createdAt;
}
