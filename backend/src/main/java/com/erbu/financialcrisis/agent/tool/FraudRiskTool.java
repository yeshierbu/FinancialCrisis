package com.erbu.financialcrisis.agent.tool;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import com.erbu.financialcrisis.domain.entity.RiskBlacklistHit;
import com.erbu.financialcrisis.mapper.RiskBlacklistMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 确定性反欺诈工具。整合 MySQL 精确黑名单、格式检查和可配置风险阈值。
 */

/**
 * 用几条可解释规则给申请打反欺诈风险分，记录命中的风险标签和规则说明，然后输出一个结构化的反欺诈结果。
 */
@Component
public class FraudRiskTool {

    private final RiskBlacklistMapper blacklistMapper;
    private final int baseScore;
    private final int blacklistScore;
    private final int suspiciousMobileScore;
    private final BigDecimal highAmountThreshold;
    private final int shortWorkYears;
    private final int highAmountScore;
    private final int incompleteDocumentScore;
    private final int mediumRiskThreshold;
    private final int highRiskThreshold;

    public FraudRiskTool(
            RiskBlacklistMapper blacklistMapper,
            @Value("${approval.fraud.base-score:20}") int baseScore,
            @Value("${approval.fraud.blacklist-score:75}") int blacklistScore,
            @Value("${approval.fraud.suspicious-mobile-score:20}") int suspiciousMobileScore,
            @Value("${approval.fraud.high-amount-threshold:100000}") BigDecimal highAmountThreshold,
            @Value("${approval.fraud.short-work-years:2}") int shortWorkYears,
            @Value("${approval.fraud.high-amount-score:25}") int highAmountScore,
            @Value("${approval.fraud.incomplete-document-score:30}") int incompleteDocumentScore,
            @Value("${approval.fraud.medium-risk-threshold:50}") int mediumRiskThreshold,
            @Value("${approval.fraud.high-risk-threshold:80}") int highRiskThreshold) {
        this.blacklistMapper = blacklistMapper;
        this.baseScore = baseScore;
        this.blacklistScore = blacklistScore;
        this.suspiciousMobileScore = suspiciousMobileScore;
        this.highAmountThreshold = highAmountThreshold;
        this.shortWorkYears = shortWorkYears;
        this.highAmountScore = highAmountScore;
        this.incompleteDocumentScore = incompleteDocumentScore;
        this.mediumRiskThreshold = mediumRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
    }

    /** 根据申请信息和材料完整性计算风险分数、标签及建议动作。 */
    public FraudRiskResult evaluate(LoanApplication application, DocumentIntakeResult documentResult) {
        List<String> riskTags = new ArrayList<>();
        List<String> ruleHits = new ArrayList<>();
        int score = baseScore;
        String suggestedAction = "PASS";

        /*
         * 这里的规则都故意写得简单、可解释。金融场景里第一版最重要的不是规则多复杂，
         * 而是每个风险标签都能回放：为什么命中、命中了什么规则、后续动作是什么。
         */
        /**
         * 命中身份证后四位黑名单直接拒绝，风险分加75分
         */
        List<RiskBlacklistHit> blacklistHits = new ArrayList<>();
        findBlacklistHit("ID_CARD", application.getIdCardNo(), blacklistHits);
        findBlacklistHit("MOBILE", application.getMobile(), blacklistHits);
        if (!blacklistHits.isEmpty()) {
            score += blacklistScore;
            suggestedAction = "REJECT";
            riskTags.add("EXACT_BLACKLIST_MATCH");
            blacklistHits.forEach(hit -> ruleHits.add(
                    "RISK_001: " + hit.getSubjectType() + " 命中精确黑名单，reason=" + hit.getReasonCode()));
        }
/**
 * 手机号异常加风险分
 */
        if (mobileSuspicious(application.getMobile())) {
            score += suspiciousMobileScore;
            riskTags.add("MOBILE_FORMAT_ABNORMAL");
            ruleHits.add("RISK_002: 手机号格式异常");
        }

        /**
         * 判断申请金额是否较高，同时工作年限是否太短
         */
        if (highAmountLowWorkYears(application)) {
            score += highAmountScore;
            riskTags.add("HIGH_AMOUNT_LOW_WORK_YEARS");
            ruleHits.add("RISK_003: 申请金额较高且工作年限较短");
        }
/**
 * 如果材料采集工具判断需要补件，就提高风险分并建议人工复核
 */
        if (Boolean.TRUE.equals(documentResult.getNeedSupplement())) {
            score += incompleteDocumentScore;
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

    /** 敏感标识在应用侧规范化并哈希，MySQL 只做精确等值查询。 */
    private void findBlacklistHit(String subjectType, String rawValue, List<RiskBlacklistHit> hits) {
        if (rawValue == null || rawValue.isBlank()) return;
        RiskBlacklistHit hit = blacklistMapper.selectActive(subjectType, sha256(normalize(rawValue)));
        if (hit != null) hits.add(hit);
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", "").toUpperCase();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("黑名单标识哈希失败", ex);
        }
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
                && application.getLoanAmount().compareTo(highAmountThreshold) > 0
                && workYears < shortWorkYears;
    }

    /**
     * 根据最终的分给出风险标准
     * @param score
     * @return
     */
    private RiskLevel resolveRiskLevel(int score) {
        if (score >= highRiskThreshold) {
            return RiskLevel.HIGH;
        }
        if (score >= mediumRiskThreshold) {
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
