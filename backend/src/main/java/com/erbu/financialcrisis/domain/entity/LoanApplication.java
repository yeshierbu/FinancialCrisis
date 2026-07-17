package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款申请主表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {

    private Long applicationId;
    private String applicationNo;
    private String productCode;
    private String applicantName;
    private String idCardNo;
    private String mobile;
    private BigDecimal loanAmount;
    private Integer loanTerm;
    private String employmentType;
    private String companyName;
    private Integer workYears;
    private ApplicationStatus status;
    private String currentStep;
    private String channelCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
