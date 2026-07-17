package com.erbu.financialcrisis.agent.artifact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** ReviewWorker 对风险报告的独立复核工件。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReport {
    private Boolean accepted;
    private List<String> contradictions;
    private List<String> unsupportedClaims;
    private List<String> revisionInstructions;
    private String recommendedAction;
    private BigDecimal confidence;
    private String summary;
}
