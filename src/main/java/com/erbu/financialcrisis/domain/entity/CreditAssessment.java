package com.erbu.financialcrisis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 征信评估结果表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditAssessment {

    private Long id;
    private Long applicationId;
    private Integer creditScore;
    private BigDecimal totalLiability;
    private Integer overdueCount;
    private BigDecimal creditCardUtilization;
    private Integer loanAccountCount;
    private String externalReportNo;
    private LocalDateTime assessmentTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
