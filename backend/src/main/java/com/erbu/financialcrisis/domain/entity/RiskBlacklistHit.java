package com.erbu.financialcrisis.domain.entity;

import lombok.Data;

/** 黑名单精确命中结果，不返回原始敏感标识。 */
@Data
public class RiskBlacklistHit {
    private String subjectType;
    private String riskLevel;
    private String reasonCode;
    private String source;
}
