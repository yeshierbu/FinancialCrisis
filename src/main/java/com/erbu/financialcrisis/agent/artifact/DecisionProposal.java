package com.erbu.financialcrisis.agent.artifact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** DecisionWorker 的审批建议；真正落库前仍需 PolicyGuard 校验。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionProposal {
    private String decision;
    private BigDecimal approvedAmount;
    private Integer approvedTerm;
    private List<String> reasonCodes;
    private List<String> evidence;
    private List<String> policyReferences;
    private BigDecimal confidence;
    private String explanation;
}
