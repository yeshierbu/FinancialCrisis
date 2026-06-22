package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 反欺诈风控 Agent。
 * 后续可以在这里整合黑名单、设备指纹、多头借贷和实名校验工具。
 */
@Component
public class FraudRiskAgent {

    public FraudRiskResult evaluate(LoanApplication application, DocumentIntakeResult documentResult) {
        List<String> riskTags = new ArrayList<>();
        List<String> ruleHits = new ArrayList<>();
        int score = 20;
        String suggestedAction = "PASS";

        /*
         * 这里的规则都故意写得简单、可解释。金融场景里第一版最重要的不是规则多复杂，
         * 而是每个风险标签都能回放：为什么命中、命中了什么规则、后续动作是什么。
         */
        if (hitMockBlacklist(application.getIdCardNo())) {
            score += 75;
            suggestedAction = "REJECT";
            riskTags.add("ID_CARD_BLACKLIST");
            ruleHits.add("RISK_001: 身份证尾号命中本地模拟黑名单");
        }

        if (mobileSuspicious(application.getMobile())) {
            score += 20;
            riskTags.add("MOBILE_FORMAT_ABNORMAL");
            ruleHits.add("RISK_002: 手机号格式异常");
        }

        if (highAmountLowWorkYears(application)) {
            score += 25;
            riskTags.add("HIGH_AMOUNT_LOW_WORK_YEARS");
            ruleHits.add("RISK_003: 申请金额较高且工作年限较短");
        }

        if (Boolean.TRUE.equals(documentResult.getNeedSupplement())) {
            score += 30;
            suggestedAction = "MANUAL_REVIEW";
            riskTags.add("DOCUMENT_INCOMPLETE");
            ruleHits.add("RISK_004: 材料不完整，需要补件或人工确认");
        }

        RiskLevel riskLevel = resolveRiskLevel(score);
        if (riskLevel == RiskLevel.HIGH && !"REJECT".equals(suggestedAction)) {
            suggestedAction = "MANUAL_REVIEW";
        }

        LocalDateTime now = LocalDateTime.now();
        return new FraudRiskResult(
                null,
                application.getApplicationId(),
                riskLevel,
                BigDecimal.valueOf(Math.min(score, 100)),
                toJsonArray(riskTags),
                toJsonArray(ruleHits),
                suggestedAction,
                now,
                now
        );
    }

    private boolean hitMockBlacklist(String idCardNo) {
        return idCardNo != null && (idCardNo.endsWith("9999") || idCardNo.endsWith("0000"));
    }

    private boolean mobileSuspicious(String mobile) {
        return mobile == null || !mobile.matches("^1[3-9]\\d{9}$");
    }

    private boolean highAmountLowWorkYears(LoanApplication application) {
        int workYears = application.getWorkYears() == null ? 0 : application.getWorkYears();
        return application.getLoanAmount() != null
                && application.getLoanAmount().compareTo(new BigDecimal("100000")) > 0
                && workYears < 2;
    }

    private RiskLevel resolveRiskLevel(int score) {
        if (score >= 80) {
            return RiskLevel.HIGH;
        }
        if (score >= 50) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String toJsonArray(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
                .reduce((left, right) -> left + "," + right)
                .map(content -> "[" + content + "]")
                .orElse("[]");
    }
}
