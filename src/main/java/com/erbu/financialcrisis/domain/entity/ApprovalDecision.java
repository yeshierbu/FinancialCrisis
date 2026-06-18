package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.DecisionResult;

import java.math.BigDecimal;

/**
 * 审批结果实体。
 * 这里承接合规决策 Agent 或人工复核给出的最终结构化结果。
 */
public class ApprovalDecision {

    private Long applicationId;
    private DecisionResult decisionResult;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer loanTerm;
    private String rejectReasonCode;
    private String decisionExplanation;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public DecisionResult getDecisionResult() {
        return decisionResult;
    }

    public void setDecisionResult(DecisionResult decisionResult) {
        this.decisionResult = decisionResult;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getLoanTerm() {
        return loanTerm;
    }

    public void setLoanTerm(Integer loanTerm) {
        this.loanTerm = loanTerm;
    }

    public String getRejectReasonCode() {
        return rejectReasonCode;
    }

    public void setRejectReasonCode(String rejectReasonCode) {
        this.rejectReasonCode = rejectReasonCode;
    }

    public String getDecisionExplanation() {
        return decisionExplanation;
    }

    public void setDecisionExplanation(String decisionExplanation) {
        this.decisionExplanation = decisionExplanation;
    }
}
