package com.erbu.financialcrisis.agent.guard;

import com.erbu.financialcrisis.agent.artifact.DecisionProposal;
import com.erbu.financialcrisis.agent.artifact.ReviewReport;
import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyGuardTests {

    private final PolicyGuard guard = new PolicyGuard(new ObjectMapper());

    @Test
    void shouldRejectExactBlacklistEvenWhenLlmApproves() {
        ApprovalDecision result = guard.validate(application(), completeDocument(),
                fraud("REJECT"), repayment("0.20"), acceptedReview(), approvedProposal("50000"));

        assertThat(result.getDecisionResult()).isEqualTo(DecisionResult.REJECTED);
        assertThat(result.getRejectReasonCode()).isEqualTo("BLACKLIST_HIT");
    }

    @Test
    void shouldSendInvalidApprovedAmountToManualReview() {
        ApprovalDecision result = guard.validate(application(), completeDocument(),
                fraud("PASS"), repayment("0.20"), acceptedReview(), approvedProposal("120000"));

        assertThat(result.getDecisionResult()).isEqualTo(DecisionResult.MANUAL_REVIEW);
        assertThat(result.getRejectReasonCode()).isEqualTo("LLM_PROPOSAL_INVALID");
    }

    private LoanApplication application() {
        LoanApplication value = new LoanApplication();
        value.setApplicationId(1L);
        value.setLoanAmount(new BigDecimal("100000"));
        value.setLoanTerm(12);
        return value;
    }

    private DocumentIntakeResult completeDocument() {
        return new DocumentIntakeResult(true, List.of(), BigDecimal.ONE, false, "complete");
    }

    private FraudRiskResult fraud(String action) {
        FraudRiskResult value = new FraudRiskResult();
        value.setRiskLevel(RiskLevel.LOW);
        value.setSuggestedAction(action);
        return value;
    }

    private RepaymentCapacityResult repayment(String dti) {
        RepaymentCapacityResult value = new RepaymentCapacityResult();
        value.setDti(new BigDecimal(dti));
        value.setRecommendedCreditLimit(new BigDecimal("80000"));
        return value;
    }

    private ReviewReport acceptedReview() {
        return new ReviewReport(true, List.of(), List.of(), List.of(),
                "PASS", new BigDecimal("0.90"), "accepted");
    }

    private DecisionProposal approvedProposal(String amount) {
        return new DecisionProposal("APPROVED", new BigDecimal(amount), 12,
                List.of("LOW_RISK"), List.of("tool-result-1"), List.of(),
                new BigDecimal("0.90"), "approved");
    }
}
