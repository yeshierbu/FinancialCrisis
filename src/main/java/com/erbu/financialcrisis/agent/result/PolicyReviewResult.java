package com.erbu.financialcrisis.agent.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 审查 Agent 对多个专业 Agent 结果的交叉复核结论。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyReviewResult {

    private Boolean conflictDetected;
    private Boolean needManualReview;
    private String recommendedAction;
    private BigDecimal confidence;
    private List<String> evidence;
    private String summary;
    private String source;
}
