package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String applicationNo;
    private String idCardNo;
    private String mobile;
    private BigDecimal loanAmount;
    private Integer loanTerm;
    private String employmentType;
    private String companyName;
    private Integer workYears;
    private LocalDateTime appliedAt;
    private String riskSummary;
    private BigDecimal riskScore;
    private BigDecimal stableMonthlyIncome;
    private BigDecimal monthlyDebtPayment;
    private BigDecimal dti;
    private BigDecimal recommendedCreditLimit;
    private ReviewStatus reviewStatus;
    private DecisionResult decisionResult;
    private BigDecimal approvedAmount;
    private String rejectReasonCode;
    private String decisionExplanation;
}
