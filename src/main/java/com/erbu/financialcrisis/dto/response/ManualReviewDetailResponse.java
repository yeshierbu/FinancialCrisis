package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 人工复核详情响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewDetailResponse {

    private Long applicationId;
    private String ticketNo;
    private String productCode;
    private String applicantName;
    private String riskSummary;
    private ReviewStatus reviewStatus;
    private DecisionResult decisionResult;
    private BigDecimal approvedAmount;
    private String rejectReasonCode;
    private String decisionExplanation;
}
