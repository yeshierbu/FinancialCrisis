package com.erbu.financialcrisis.agent.worker;

import com.erbu.financialcrisis.agent.artifact.ReviewReport;
import com.erbu.financialcrisis.agent.artifact.RiskReport;
import com.erbu.financialcrisis.agent.runtime.StructuredLlmClient;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 独立复核 Worker，不读取 RiskWorker 的聊天历史，只检查其结构化工件与原始事实。 */
@Component
public class ReviewWorker {
    private final StructuredLlmClient llm;
    private final ObjectMapper objectMapper;

    public ReviewWorker(StructuredLlmClient llm, ObjectMapper objectMapper) {
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    public ReviewReport review(RiskReport riskReport, FraudRiskResult fraud,
                               RepaymentCapacityResult repayment) {
        try {
            String input = objectMapper.writeValueAsString(Map.of(
                    "riskReport", riskReport,
                    "fraudToolResult", fraud,
                    "repaymentToolResult", repayment
            ));
            return llm.generate(
                    "你是独立信贷复核 Agent。检查风险报告是否忠于工具事实、是否存在无证据结论、"
                            + "内部矛盾、遗漏风险或伪造政策引用。不得仅因措辞不同要求返工。",
                    "请复核以下内容并返回 ReviewReport JSON：\n" + input
                            + "\n字段：accepted,contradictions,unsupportedClaims,revisionInstructions,"
                            + "recommendedAction,confidence,summary。recommendedAction 只能是 PASS、MANUAL_REVIEW、REJECT。",
                    ReviewReport.class
            );
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("ReviewWorker 构建上下文失败", ex);
        }
    }
}
