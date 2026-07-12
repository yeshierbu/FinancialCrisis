package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.agent.collaboration.ApprovalCaseContext;
import com.erbu.financialcrisis.agent.result.PolicyReviewResult;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmApprovalAgentStrictModeTests {

    @Test
    void missingApiKeyMustNotUseRuleFallback() {
        LlmApprovalAgent agent = new LlmApprovalAgent(
                mock(ChatLanguageModel.class), new ObjectMapper(), "", "deepseek-test", true
        );

        assertThrows(IllegalStateException.class,
                () -> agent.review(null, null, null, null));
    }

    @Test
    void deepSeekFailureMustAbortLlmReview() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        when(model.generate(anyList())).thenThrow(new RuntimeException("remote API unavailable"));

        FraudRiskResult fraud = new FraudRiskResult();
        fraud.setRiskLevel(RiskLevel.LOW);
        fraud.setRiskScore(new BigDecimal("20"));
        fraud.setSuggestedAction("PASS");

        RepaymentCapacityResult repayment = new RepaymentCapacityResult();
        repayment.setDti(new BigDecimal("0.30"));
        repayment.setFoir(new BigDecimal("0.30"));
        repayment.setRecommendedCreditLimit(new BigDecimal("50000"));

        PolicyReviewResult ruleReview = new PolicyReviewResult(
                false, false, "PASS", new BigDecimal("0.90"), List.of(), "rule review", "RULE"
        );
        LlmApprovalAgent agent = new LlmApprovalAgent(
                model, new ObjectMapper(), "test-api-key", "deepseek-test", true
        );

        assertThrows(IllegalStateException.class, () -> agent.review(
                new ApprovalCaseContext(1L), fraud, repayment, ruleReview
        ));
    }
}
