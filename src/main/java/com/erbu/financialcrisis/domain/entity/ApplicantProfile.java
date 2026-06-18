package com.erbu.financialcrisis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 申请人画像表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantProfile {

    private Long id;
    private Long applicationId;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String maritalStatus;
    private String educationLevel;
    private String occupation;
    private String employerName;
    private BigDecimal annualIncome;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
