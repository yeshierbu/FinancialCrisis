package com.erbu.financialcrisis.agent.artifact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** DocumentAnalysisWorker 对 OCR 文本的结构化理解结果。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisReport {
    private Boolean documentComplete;
    private Boolean identityConsistent;
    private List<String> incomeEvidence;
    private List<String> evidence;
    private List<String> anomalies;
    private List<String> missingEvidence;
    private String recommendedAction;
    private BigDecimal confidence;
    private String summary;
}
