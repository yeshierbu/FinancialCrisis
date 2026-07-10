package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.agent.collaboration.ApprovalCaseContext;
import com.erbu.financialcrisis.agent.result.PolicyReviewResult;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 审查 Agent。
 *
 * <p>它不重复计算反欺诈和偿债指标，而是读取共享案件上下文，专门检查专业 Agent 之间是否存在
 * 冲突、边界风险或证据不足，再把建议交给合规决策 Agent。</p>
 */
@Component
public class ApprovalCriticAgent {

    public PolicyReviewResult review(ApprovalCaseContext context,
                                     FraudRiskResult fraudRiskResult,
                                     RepaymentCapacityResult repaymentResult) {
        List<String> evidence = new ArrayList<>();
        boolean hardReject = "REJECT".equals(fraudRiskResult.getSuggestedAction())
                || repaymentResult.getDti().compareTo(new BigDecimal("0.70")) > 0;
        boolean boundaryRisk = fraudRiskResult.getRiskLevel() != RiskLevel.LOW
                || repaymentResult.getDti().compareTo(new BigDecimal("0.50")) > 0;
        boolean conflictDetected = (fraudRiskResult.getRiskLevel() == RiskLevel.LOW
                && repaymentResult.getDti().compareTo(new BigDecimal("0.50")) > 0)
                || (fraudRiskResult.getRiskLevel() == RiskLevel.HIGH
                && repaymentResult.getDti().compareTo(new BigDecimal("0.50")) <= 0);

        evidence.add("已读取共享案件上下文中的 " + context.getFindings().size() + " 条 Agent 发现");
        evidence.add("反欺诈风险等级：" + fraudRiskResult.getRiskLevel());
        evidence.add("偿债能力 DTI：" + repaymentResult.getDti());
        if (conflictDetected) {
            evidence.add("反欺诈风险与偿债能力结论处于不一致区间");
        }

        String action;
        String summary;
        BigDecimal confidence;
        if (hardReject) {
            action = "REJECT";
            summary = "审查 Agent 确认存在硬性拒绝条件，建议交由规则决策拒绝";
            confidence = new BigDecimal("0.96");
        } else if (boundaryRisk || conflictDetected) {
            action = "MANUAL_REVIEW";
            summary = conflictDetected
                    ? "专业 Agent 结论存在边界冲突，建议人工复核"
                    : "风险或偿债指标处于边界区间，建议人工复核";
            confidence = conflictDetected ? new BigDecimal("0.74") : new BigDecimal("0.88");
        } else {
            action = "PASS";
            summary = "各专业 Agent 结论一致，未发现需要人工介入的边界风险";
            confidence = new BigDecimal("0.93");
        }

        return new PolicyReviewResult(
                conflictDetected,
                !"PASS".equals(action),
                action,
                confidence,
                evidence,
                summary,
                "RULE_CRITIC"
        );
    }
}
