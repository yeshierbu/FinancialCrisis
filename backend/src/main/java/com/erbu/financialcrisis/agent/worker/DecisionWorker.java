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

/** 最终决策 Worker。它提出决定，但不能绕过 PolicyGuard 直接改变申请状态。 */
@Component
public class DecisionWorker {
    private final StructuredLlmClient llm;
    private final ObjectMapper objectMapper;

    public DecisionWorker(StructuredLlmClient llm, ObjectMapper objectMapper) {
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    public DecisionProposal decide(LoanApplication application, RiskReport risk,
                                   ReviewReport review, RepaymentCapacityResult repayment) {
        try {
            String input = objectMapper.writeValueAsString(Map.of(
                    "application", application,
                    "riskReport", risk,
                    "reviewReport", review,
                    "maximumRecommendedAmount", repayment.getRecommendedCreditLimit()
            ));
            return llm.generate(
                    "你是信贷审批决策 Agent。基于已审查的风险工件形成决定。decision 只能是 APPROVED、"
                            + "REJECTED、MANUAL_REVIEW；批准金额不得超过申请金额和 maximumRecommendedAmount。",
                    "请返回 DecisionProposal JSON：\n" + input
                            + "\n字段：decision,approvedAmount,approvedTerm,reasonCodes,evidence,"
                            + "policyReferences,confidence,explanation。证据不足或复核未通过时必须选择 MANUAL_REVIEW。",
                    DecisionProposal.class
            );
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("DecisionWorker 构建上下文失败", ex);
        }
    }
}
