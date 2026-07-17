package com.erbu.financialcrisis.agent.artifact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** RiskWorker 写入共享上下文的结构化风险工件。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskReport {
    private String riskLevel;
    private BigDecimal riskScore;
    private String recommendedAction;
    private List<String> claims;
    private List<String> evidence;
    private List<String> missingEvidence;
    private List<String> policyReferences;
    private BigDecimal confidence;
    private String summary;
}
