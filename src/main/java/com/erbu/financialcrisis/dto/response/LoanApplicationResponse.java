package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款申请响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    private Long applicationId;
    private String applicationNo;
    private String productCode;
    private String applicantName;
    private BigDecimal loanAmount;
    private Integer loanTerm;
    private ApplicationStatus status;
    private String currentStep;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
