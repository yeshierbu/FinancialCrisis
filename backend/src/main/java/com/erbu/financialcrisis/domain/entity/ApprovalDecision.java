package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.DecisionResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批决策结果表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {

    private Long id;
    private Long applicationId;
    private DecisionResult decisionResult;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer loanTerm;
    private String rejectReasonCode;
    private String decisionExplanation;
    private String policyReferencesJson;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
