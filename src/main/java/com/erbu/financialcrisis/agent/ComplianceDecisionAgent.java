package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.agent.result.PolicyReviewResult;
import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合规决策 Agent。
 * 这一层最终应该接规则引擎、RAG 检索和决策解释生成逻辑。
 */
@Component
public class ComplianceDecisionAgent {

    /** 按硬性拒绝、人工复核、自动通过的优先级生成最终审批决定。 */
    /**
     *
     * @param application   贷款申请主信息
     * @param documentResult  材料采集结果
     * @param fraudRiskResult  反欺诈结果
     * @param repaymentResult  偿债能力结果
     * @param policyReviewResult  审查 Agent 或 LLM 复核结果
     * @return
     */
    public ApprovalDecision decide(LoanApplication application,
                                   DocumentIntakeResult documentResult,
                                   FraudRiskResult fraudRiskResult,
                                   RepaymentCapacityResult repaymentResult,
                                   PolicyReviewResult policyReviewResult){
        /*
         * 决策顺序很重要：先处理硬性不可继续条件，再判断人工复核，最后才自动通过。
         * 这样能避免“大模型/解释文本”覆盖硬规则，也方便后续把这些判断迁移到规则引擎。
         */
        /**
         * 如果申请材料需要补充，就直接生成一个“人工复核”决策，不再进行后面的反欺诈，偿债能力，自动通过逻辑
         */
        if (Boolean.TRUE.equals(documentResult.getNeedSupplement())) {
            return buildDecision(
                    application,
                    DecisionResult.MANUAL_REVIEW,
                    BigDecimal.ZERO,
                    "DOCUMENT_PENDING",
                    "材料不完整，需补件或人工确认后再继续审批"
            );
        }
        /**
         * 反欺诈硬性拒绝，表示立即返回最终审批结果，不再继续后面的偿债能力、人工复核、自动通过判断
         */
        if ("REJECT".equals(fraudRiskResult.getSuggestedAction())) {
            return buildDecision(
                    application,
                    DecisionResult.REJECTED,
                    BigDecimal.ZERO,
                    "BLACKLIST_HIT",
                    "反欺诈规则命中本地模拟黑名单，触发硬性拒绝策略"
            );
        }
        /**
         * 如果审查 Agent 认为申请存在硬性风险条件，系统直接拒绝，不再继续后面的人工复核或自动通过判断
         */
        if ("REJECT".equals(policyReviewResult.getRecommendedAction())) {
            return buildDecision(
                    application,
                    DecisionResult.REJECTED,
                    BigDecimal.ZERO,
                    "POLICY_REVIEW_REJECT",
                    "审查 Agent 确认存在硬性风险条件，规则决策拒绝"
            );
        }
/**
 *如果申请人的债务收入比超过 70%，说明偿债压力过高，系统直接拒绝
 */
        if (repaymentResult.getDti().compareTo(new BigDecimal("0.70")) > 0) {
            return buildDecision(
                    application,
                    DecisionResult.REJECTED,
                    BigDecimal.ZERO,
                    "DTI_TOO_HIGH",
                    "债务收入比超过 70%，偿债压力过高，不满足自动准入条件"
            );
        }
/**
 * 满足一个条件进行人工复核
 */
        if (Boolean.TRUE.equals(policyReviewResult.getNeedManualReview())
                || fraudRiskResult.getRiskLevel() == RiskLevel.MEDIUM
                || fraudRiskResult.getRiskLevel() == RiskLevel.HIGH
                || repaymentResult.getDti().compareTo(new BigDecimal("0.50")) > 0) {
            return buildDecision(
                    application,
                    DecisionResult.MANUAL_REVIEW,
                    BigDecimal.ZERO,
                    "NEED_MANUAL_CONFIRM",
                    "审查 Agent 发现风险边界或结论冲突，转人工复核确认：" + policyReviewResult.getSummary()
            );
        }
        /**
         * 批准金额不能超过用户申请金额getLoanAmount()，也不能超过系统根据偿债能力计算出的推荐额度getRecommendedCreditLimit()
         */
        BigDecimal approvedAmount = application.getLoanAmount().min(repaymentResult.getRecommendedCreditLimit());
        return buildDecision(
                application,
                DecisionResult.APPROVED,
                approvedAmount,
                null,
                "风险等级低且偿债能力满足规则，系统自动审批通过"
        );
    }

    /**
     *
     * @param application
     * @param decisionResult
     * @param approvedAmount
     * @param rejectReasonCode
     * @param decisionExplanation
     * @return
     */
    private ApprovalDecision buildDecision(LoanApplication application,
                                           DecisionResult decisionResult,
                                           BigDecimal approvedAmount,
                                           String rejectReasonCode,
                                           String decisionExplanation) {
        LocalDateTime now = LocalDateTime.now();
        return new ApprovalDecision(
                null,
                application.getApplicationId(),
                decisionResult,
                approvedAmount,
                new BigDecimal("10.80"),
                application.getLoanTerm(),
                rejectReasonCode,
                decisionExplanation,
                "[\"POLICY_CONSUMER_LOAN_001#AUTO_APPROVAL_BOUNDARY\"]",
                "RULE_AGENT",
                now,
                now,
                now
        );
    }
}
