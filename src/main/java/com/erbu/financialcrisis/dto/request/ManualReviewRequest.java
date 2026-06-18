package com.erbu.financialcrisis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 人工复核请求。
 * 同一个 DTO 既可以用于通过，也可以用于拒绝，具体由接口路径决定动作。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualReviewRequest {

    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer loanTerm;

    @NotBlank(message = "复核意见不能为空")
    private String reviewComment;

    private String rejectReasonCode;
}
