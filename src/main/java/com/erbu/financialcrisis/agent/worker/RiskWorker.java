package com.erbu.financialcrisis.agent.worker;

import com.erbu.financialcrisis.agent.artifact.RiskReport;
import com.erbu.financialcrisis.agent.runtime.StructuredLlmClient;
import com.erbu.financialcrisis.agent.tool.PolicySearchTool;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.knowledge.PolicyEvidence;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 综合风险 Worker。确定性组件提供黑名单和偿债计算事实，LLM 负责综合证据、
 * 检索政策并形成案件级判断。
 */
@Component
public class RiskWorker {
    private final StructuredLlmClient llm;
    private final PolicySearchTool policySearchTool;
    private final ObjectMapper objectMapper;

    public RiskWorker(StructuredLlmClient llm, PolicySearchTool policySearchTool,
                      ObjectMapper objectMapper) {
        this.llm = llm;
        this.policySearchTool = policySearchTool;
        this.objectMapper = objectMapper;
    }

    public RiskReport analyze(LoanApplication application,
                              FraudRiskResult fraud,
                              RepaymentCapacityResult repayment,
                              List<String> documentEvidence,
                              List<String> revisionInstructions) {
        try {
            List<PolicyEvidence> policies = policySearchTool.search(
                    "消费贷反欺诈、偿债能力、DTI、自动审批和人工复核政策", "CONSUMER_LOAN");
            Map<String, Object> input = Map.of(
                    "application", application,
                    "fraudToolResult", fraud,
                    "repaymentToolResult", repayment,
                    "documentEvidence", documentEvidence,
                    "retrievedPolicies", policies,
                    "reviewRevisionInstructions", revisionInstructions == null ? List.of() : revisionInstructions
            );
            return llm.generate(
                    "你是信贷综合风险分析 Agent。工具结果是事实，不得篡改；政策检索结果是依据，必须引用 documentId/section。"
                            + "分析欺诈风险、偿债能力、证据缺口和相互冲突。recommendedAction 只能是 PASS、MANUAL_REVIEW、REJECT。",
                    "请分析以下案件并返回 RiskReport JSON：\n" + objectMapper.writeValueAsString(input)
                            + "\n字段：riskLevel,riskScore,recommendedAction,claims,evidence,missingEvidence,"
                            + "policyReferences,confidence,summary。没有政策证据时 policyReferences 返回空数组，不得编造。",
                    RiskReport.class
            );
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("RiskWorker 构建上下文失败", ex);
        }
    }
}
