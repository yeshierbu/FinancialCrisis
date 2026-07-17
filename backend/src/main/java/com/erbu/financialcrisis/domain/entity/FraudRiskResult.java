package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 反欺诈风控结果表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudRiskResult {

    private Long id;
    private Long applicationId;
    private RiskLevel riskLevel;
    private BigDecimal riskScore;
    private String riskTagsJson;
    private String ruleHitsJson;
    private String suggestedAction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
