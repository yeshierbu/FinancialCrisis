package com.erbu.financialcrisis.agent.worker;

import com.erbu.financialcrisis.agent.artifact.DecisionProposal;
import com.erbu.financialcrisis.agent.artifact.ReviewReport;
import com.erbu.financialcrisis.agent.artifact.RiskReport;
import com.erbu.financialcrisis.agent.runtime.StructuredLlmClient;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 最终决策 Worker。
 DecisionWorker 的作用是把前面链路产生的关键结果汇总成结构化 JSON 上下文，
 然后通过 systemPrompt 和 userPrompt 交给 LLM，让 LLM 生成一个审批建议。
 */
@Component
public class DecisionWorker {
    /** 统一的结构化 LLM 调用入口，负责模型调用和 JSON 反序列化。 */
    private final StructuredLlmClient llm;

    /** 用于把审批上下文序列化为 JSON，作为 LLM 的输入材料。 */
    private final ObjectMapper objectMapper;

    public DecisionWorker(StructuredLlmClient llm, ObjectMapper objectMapper) {
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据已完成的风险评估和复核结果生成审批建议。
     *
     * <p>方法返回的是 LLM 的建议对象，不是最终审批结果。调用方需要继续把该建议传给
     * PolicyGuard，由确定性规则检查金额上限、证据充分性、置信度和硬拒绝条件。</p>
     */
    public DecisionProposal decide(LoanApplication application, RiskReport risk,
                                   ReviewReport review, RepaymentCapacityResult repayment) {
        try {
            // 只把最终决策需要的信息传给模型，减少无关字段对决策的干扰。
            String input = objectMapper.writeValueAsString(Map.of(
                    "application", application,
                    "riskReport", risk,
                    "reviewReport", review,
                    "maximumRecommendedAmount", repayment.getRecommendedCreditLimit()
            ));

            // 系统提示词约束模型的角色和硬边界；用户提示词要求返回固定 JSON 结构。
            return llm.generate(
                    "你是信贷审批决策 Agent。基于已审查的风险工件形成决定。decision 只能是 APPROVED、"
                            + "REJECTED、MANUAL_REVIEW；批准金额不得超过申请金额和 maximumRecommendedAmount。",
                    "请返回 DecisionProposal JSON：\n" + input
                            + "\n字段：decision,approvedAmount,approvedTerm,reasonCodes,evidence,"
                            + "policyReferences,confidence,explanation。证据不足或复核未通过时必须选择 MANUAL_REVIEW。",
                    DecisionProposal.class
            );
        } catch (Exception ex) {
            // LLM 调用失败已经是 IllegalStateException 时保留原始异常语义，避免重复包装。
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("DecisionWorker 构建上下文失败", ex);
        }
    }
}
