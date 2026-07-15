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

/**
 * 用几条可解释规则给申请打反欺诈风险分，记录命中的风险标签和规则说明，然后输出一个结构化的反欺诈结果。
 */
@Component
public class FraudRiskAgent {

    /** 根据申请信息和材料完整性计算风险分数、标签及建议动作。 */
    public FraudRiskResult evaluate(LoanApplication application, DocumentIntakeResult documentResult) {
        List<String> riskTags = new ArrayList<>();
        List<String> ruleHits = new ArrayList<>();
        int score = 20;
        String suggestedAction = "PASS";

        /*
         * 这里的规则都故意写得简单、可解释。金融场景里第一版最重要的不是规则多复杂，
         * 而是每个风险标签都能回放：为什么命中、命中了什么规则、后续动作是什么。
         */
        /**
         * 命中身份证后四位黑名单直接拒绝，风险分加75分
         */
        if (hitMockBlacklist(application.getIdCardNo())) {
            score += 75;
            suggestedAction = "REJECT";
            riskTags.add("ID_CARD_BLACKLIST");
            ruleHits.add("RISK_001: 身份证尾号命中本地模拟黑名单");
        }
/**
 * 手机号异常加风险分
 */
        if (mobileSuspicious(application.getMobile())) {
            score += 20;
            riskTags.add("MOBILE_FORMAT_ABNORMAL");
            ruleHits.add("RISK_002: 手机号格式异常");
        }

        /**
         * 判断申请金额是否较高，同时工作年限是否太短
         */
        if (highAmountLowWorkYears(application)) {
            score += 25;
            riskTags.add("HIGH_AMOUNT_LOW_WORK_YEARS");
            ruleHits.add("RISK_003: 申请金额较高且工作年限较短");
        }
/**
 * 如果信息采集 Agent 判断需要补件，就提高风险分并建议人工复核
 */
        if (Boolean.TRUE.equals(documentResult.getNeedSupplement())) {
            score += 30;
            suggestedAction = "MANUAL_REVIEW";
            riskTags.add("DOCUMENT_INCOMPLETE");
            ruleHits.add("RISK_004: 材料不完整，需要补件或人工确认");
        }
        /**
         * 根据测算结果判断是拒绝还是人工复核
         */
        RiskLevel riskLevel = resolveRiskLevel(score);
        if (riskLevel == RiskLevel.HIGH && !"REJECT".equals(suggestedAction)) {
            suggestedAction = "MANUAL_REVIEW";
        }
/**
 * 返回反欺诈结果
 */
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

    /**
     * mock身份证黑名单
     * @param idCardNo
     * @return
     */
    private boolean hitMockBlacklist(String idCardNo) {
        return idCardNo != null && (idCardNo.endsWith("9999") || idCardNo.endsWith("0000"));
    }

    /**
     * mock手机号黑名单
     * @param mobile
     * @return
     */
    private boolean mobileSuspicious(String mobile) {
        return mobile == null || !mobile.matches("^1[3-9]\\d{9}$");
    }

    /**
     * mock高金额短工作年限
     * @param application
     * @return
     */
    private boolean highAmountLowWorkYears(LoanApplication application) {
        int workYears = application.getWorkYears() == null ? 0 : application.getWorkYears();
        return application.getLoanAmount() != null
                && application.getLoanAmount().compareTo(new BigDecimal("100000")) > 0
                && workYears < 2;
    }

    /**
     * 根据最终的分给出风险标准
     * @param score
     * @return
     */
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
