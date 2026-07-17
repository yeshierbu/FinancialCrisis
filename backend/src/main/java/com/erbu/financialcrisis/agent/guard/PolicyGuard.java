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
 */
@Component
public class PolicyGuard {
    private static final BigDecimal MIN_AUTO_CONFIDENCE = new BigDecimal("0.80");
    private final ObjectMapper objectMapper;

    public PolicyGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ApprovalDecision validate(LoanApplication application,
                                     DocumentIntakeResult document,
                                     FraudRiskResult fraud,
                                     RepaymentCapacityResult repayment,
                                     ReviewReport review,
                                     DecisionProposal proposal) {
        if (Boolean.TRUE.equals(document.getNeedSupplement())) {
            return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                    "DOCUMENT_PENDING", "材料不完整，不能执行自动审批", proposal);
        }
        if ("REJECT".equals(fraud.getSuggestedAction())) {
            return build(application, DecisionResult.REJECTED, BigDecimal.ZERO,
                    "BLACKLIST_HIT", "精确反欺诈工具命中硬性拒绝条件", proposal);
        }
        if (repayment.getDti().compareTo(new BigDecimal("0.70")) > 0) {
            return build(application, DecisionResult.REJECTED, BigDecimal.ZERO,
                    "DTI_TOO_HIGH", "DTI 超过硬性上限", proposal);
        }
        if (!Boolean.TRUE.equals(review.getAccepted())) {
            return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                    "REVIEW_NOT_ACCEPTED", "独立复核未通过，转人工确认", proposal);
        }

        DecisionResult proposed = parseDecision(proposal.getDecision());
        if (proposed == DecisionResult.APPROVED) {
            BigDecimal maximum = application.getLoanAmount().min(repayment.getRecommendedCreditLimit());
            boolean invalidAmount = proposal.getApprovedAmount() == null
                    || proposal.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0
                    || proposal.getApprovedAmount().compareTo(maximum) > 0;
            boolean weakEvidence = proposal.getEvidence() == null || proposal.getEvidence().isEmpty();
            boolean lowConfidence = proposal.getConfidence() == null
                    || proposal.getConfidence().compareTo(MIN_AUTO_CONFIDENCE) < 0;
            if (invalidAmount || weakEvidence || lowConfidence) {
                return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                        "LLM_PROPOSAL_INVALID", "审批建议金额、证据或置信度未通过安全校验", proposal);
            }
            return build(application, DecisionResult.APPROVED, proposal.getApprovedAmount(),
                    null, proposal.getExplanation(), proposal);
        }
        if (proposed == DecisionResult.REJECTED) {
            return build(application, DecisionResult.REJECTED, BigDecimal.ZERO,
                    firstReason(proposal), proposal.getExplanation(), proposal);
        }
        return build(application, DecisionResult.MANUAL_REVIEW, BigDecimal.ZERO,
                firstReason(proposal), proposal.getExplanation(), proposal);
    }

    private DecisionResult parseDecision(String value) {
        if (value == null) return DecisionResult.MANUAL_REVIEW;
        try {
            return DecisionResult.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DecisionResult.MANUAL_REVIEW;
        }
    }

    private String firstReason(DecisionProposal proposal) {
        List<String> codes = proposal.getReasonCodes();
        return codes == null || codes.isEmpty() ? "LLM_DECISION" : codes.get(0);
    }

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

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
