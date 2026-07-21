package com.erbu.financialcrisis.agent.tool;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RiskBlacklistHit;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.mapper.RiskBlacklistMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FraudRiskToolTests {

    @Test
    void shouldUseHashedExactBlacklistInsteadOfIdSuffixRule() {
        RiskBlacklistMapper mapper = mock(RiskBlacklistMapper.class);
        RiskBlacklistHit hit = new RiskBlacklistHit();
        hit.setSubjectType("ID_CARD");
        hit.setReasonCode("KNOWN_FRAUD_IDENTITY");
        when(mapper.selectActive(eq("ID_CARD"), matches("[0-9a-f]{64}"))).thenReturn(hit);
        FraudRiskTool tool = tool(mapper);

        var result = tool.evaluate(application("310101199001011234"), completeDocuments());

        assertThat(result.getSuggestedAction()).isEqualTo("REJECT");
        assertThat(result.getRuleHitsJson()).contains("KNOWN_FRAUD_IDENTITY");
        verify(mapper).selectActive(eq("ID_CARD"), matches("[0-9a-f]{64}"));
    }

    @Test
    void idSuffixMustNotActAsLocalMockBlacklist() {
        FraudRiskTool tool = tool(mock(RiskBlacklistMapper.class));

        var result = tool.evaluate(application("310101199001019999"), completeDocuments());

        assertThat(result.getSuggestedAction()).isEqualTo("PASS");
        assertThat(result.getRiskTagsJson()).doesNotContain("BLACKLIST");
    }

    private FraudRiskTool tool(RiskBlacklistMapper mapper) {
        return new FraudRiskTool(mapper, 20, 75, 20, new BigDecimal("100000"),
                2, 25, 30, 50, 80);
    }

    private LoanApplication application(String idCard) {
        LocalDateTime now = LocalDateTime.now();
        return new LoanApplication(1L, "APP-1", "CONSUMER_LOAN_STD", "张三", idCard,
                "13800138000", new BigDecimal("50000"), 24, "FULL_TIME", "测试公司",
                3, ApplicationStatus.RISK_ANALYZING, "", "WEB", "user", now, now);
    }

    private DocumentIntakeResult completeDocuments() {
        return new DocumentIntakeResult(true, List.of(), new BigDecimal("0.95"), false, "complete");
    }
}
