package com.erbu.financialcrisis.agent.guard;

import com.erbu.financialcrisis.agent.artifact.DecisionProposal;
import com.erbu.financialcrisis.agent.artifact.ReviewReport;
import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 最薄安全护栏：不重新做完整审批，只阻止 LLM 违反精确事实、金额上限和最低证据要求。
 *
 * <p>这个类位于审批链路的最后阶段。前面的工具和 Agent 已经完成材料校验、反欺诈、
 * 还款能力评估和独立复核；PolicyGuard 只负责把这些确定性结果和 LLM 的审批建议做一次
 * 最终核对，避免模型给出越权、证据不足或违反硬规则的审批结果。</p>
 */
@Component
public class PolicyGuard {
    /** LLM 建议自动通过时必须达到的最低置信度。低于该值会转人工复核。 */
    private static final BigDecimal MIN_AUTO_CONFIDENCE = new BigDecimal("0.80");
    private final ObjectMapper objectMapper;

    public PolicyGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 校验 LLM 的审批建议，并生成最终可落库的审批决定。
     *
     * <p>校验顺序是有意设计的：先处理材料缺失、黑名单、DTI 超限、复核未通过等硬性条件；
     * 只有这些确定性规则全部通过后，才允许采纳 LLM 的通过、拒绝或人工复核建议。</p>
     */
    public ApprovalDecision validate(LoanApplication application,
                                     DocumentIntakeResult document,
                                     FraudRiskResult fraud,
                                     RepaymentCapacityResult repayment,
                                     ReviewReport review,
                                     DecisionProposal proposal) {
        // 材料未齐或 OCR 结果不可用时，不能进入自动审批，只能等待补件或人工处理。
        if (Boolean.TRUE.equals(document.getNeedSupplement())) {
            return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                    "DOCUMENT_PENDING", "材料不完整，不能执行自动审批", proposal);
        }

        // 反欺诈模块命中硬拒绝条件时，模型建议不能覆盖该结论。
        if ("REJECT".equals(fraud.getSuggestedAction())) {
            return build(application, DecisionResult.REJECTED, BigDecimal.ZERO,
                    "BLACKLIST_HIT", "精确反欺诈工具命中硬性拒绝条件", proposal);
        }

        // DTI 超过硬性上限，说明还款压力过高，直接拒绝。
        if (repayment.getDti().compareTo(new BigDecimal("0.70")) > 0) {
            return build(application, DecisionResult.REJECTED, BigDecimal.ZERO,
                    "DTI_TOO_HIGH", "DTI 超过硬性上限", proposal);
        }

        // 独立复核未接受时，不自动采纳 LLM 建议，转人工确认。
        if (!Boolean.TRUE.equals(review.getAccepted())) {
            return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                    "REVIEW_NOT_ACCEPTED", "独立复核未通过，转人工确认", proposal);
        }

        DecisionResult proposed = parseDecision(proposal.getDecision());
        if (proposed == DecisionResult.APPROVED) {
            // 批准金额不能超过用户申请金额，也不能超过还款能力工具给出的推荐授信额度。
            BigDecimal maximum = application.getLoanAmount().min(repayment.getRecommendedCreditLimit());
            boolean invalidAmount = proposal.getApprovedAmount() == null
                    || proposal.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0
                    || proposal.getApprovedAmount().compareTo(maximum) > 0;
            boolean weakEvidence = proposal.getEvidence() == null || proposal.getEvidence().isEmpty();
            boolean lowConfidence = proposal.getConfidence() == null
                    || proposal.getConfidence().compareTo(MIN_AUTO_CONFIDENCE) < 0;

            // 通过类建议必须同时满足金额有效、证据充分、置信度达标，否则降级为人工复核。
            if (invalidAmount || weakEvidence || lowConfidence) {
                return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                        "LLM_PROPOSAL_INVALID", "审批建议金额、证据或置信度未通过安全校验", proposal);
            }
            return build(application, DecisionResult.APPROVED, proposal.getApprovedAmount(),
                    null, proposal.getExplanation(), proposal);
        }

        // 拒绝类建议风险较低，可以采纳，但仍会保留模型给出的首个原因码。
        if (proposed == DecisionResult.REJECTED) {
            return build(application, DecisionResult.REJECTED, BigDecimal.ZERO,
                    firstReason(proposal), proposal.getExplanation(), proposal);
        }

        // 无法识别或明确要求人工复核的建议，统一转人工复核。
        return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                firstReason(proposal), proposal.getExplanation(), proposal);
    }

    /** 将模型输出的决策字符串转换成系统枚举；无法识别时默认转人工复核。 */
    private DecisionResult parseDecision(String value) {
        if (value == null) return DecisionResult.MANUAL_REVIEW;
        try {
            return DecisionResult.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DecisionResult.MANUAL_REVIEW;
        }
    }

    /** 取 LLM 建议中的首个原因码，方便最终审批结果记录一个主要原因。 */
    private String firstReason(DecisionProposal proposal) {
        List<String> codes = proposal.getReasonCodes();
        return codes == null || codes.isEmpty() ? "LLM_DECISION" : codes.get(0);
    }

    /** 统一构造审批决定，保证所有分支的利率、期限、来源和时间字段写法一致。 */
    private ApprovalDecision build(LoanApplication application, DecisionResult result,
                                   BigDecimal amount, String reason, String explanation,
                                   DecisionProposal proposal) {
        LocalDateTime now = LocalDateTime.now();
        return new ApprovalDecision(null, application.getApplicationId(), result, amount,
                new BigDecimal("10.80"),
                proposal.getApprovedTerm() == null ? application.getLoanTerm() : proposal.getApprovedTerm(),
                reason, explanation == null ? "LLM 审批建议已通过安全护栏处理" : explanation,
                toJson(proposal.getPolicyReferences()), "LLM_DECISION_AGENT+POLICY_GUARD",
                now, now, now);
    }

    /** 将政策依据列表保存为 JSON 字符串；序列化失败时返回空数组，避免影响主审批流程。 */
    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
