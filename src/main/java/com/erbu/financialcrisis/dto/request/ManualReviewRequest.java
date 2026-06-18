package com.erbu.financialcrisis.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 人工复核请求。
 * 同一个 DTO 既可以用于通过，也可以用于拒绝，具体由接口路径决定动作。
 */
public class ManualReviewRequest {

    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer loanTerm;

    @NotBlank(message = "复核意见不能为空")
    private String reviewComment;

    private String rejectReasonCode;

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

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getRejectReasonCode() {
        return rejectReasonCode;
    }

    public void setRejectReasonCode(String rejectReasonCode) {
        this.rejectReasonCode = rejectReasonCode;
    }
}
