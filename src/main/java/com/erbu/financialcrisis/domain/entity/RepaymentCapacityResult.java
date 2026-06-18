package com.erbu.financialcrisis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 偿债能力结果表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentCapacityResult {

    private Long id;
    private Long applicationId;
    private BigDecimal stableMonthlyIncome;
    private BigDecimal monthlyDebtPayment;
    private BigDecimal dti;
    private BigDecimal foir;
    private BigDecimal disposableIncome;
    private BigDecimal incomeStabilityScore;
    private BigDecimal maxAffordableEmi;
    private BigDecimal recommendedCreditLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
