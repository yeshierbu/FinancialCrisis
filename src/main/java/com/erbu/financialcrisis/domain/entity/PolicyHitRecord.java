package com.erbu.financialcrisis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 政策命中记录表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyHitRecord {

    private Long id;
    private Long applicationId;
    private String policyId;
    private String clauseId;
    private String policyTitle;
    private String clauseText;
    private String hitSource;
    private LocalDateTime createdAt;
}
