package com.erbu.financialcrisis.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erbu.financialcrisis.agent.collaboration.ApprovalCaseContext;
import com.erbu.financialcrisis.agent.result.PolicyReviewResult;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 审批辅助 Agent。
 *
 * <p>该 Agent 只负责对已有结构化风控结果进行解释和交叉审查，不直接替代硬规则做最终放款决定。
 * 当没有配置 API Key、接口调用失败或模型返回不合法 JSON 时，自动沿用本地规则审查结果。</p>
 */
@Component
public class LlmApprovalAgent {

    private static final String SOURCE = "DEEPSEEK_V4_FLASH";
    private static final String FALLBACK_SOURCE = "RULE_FALLBACK";

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String modelName;
    private final boolean enabled;

    public LlmApprovalAgent(ChatLanguageModel chatLanguageModel,
                            ObjectMapper objectMapper,
                            @Value("${llm.api-key:disabled}") String apiKey,
                            @Value("${llm.model:deepseek-v4-flash}") String modelName,
                            @Value("${llm.enabled:true}") boolean enabled) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.enabled = enabled;
    }

    public PolicyReviewResult review(ApprovalCaseContext context,
                                     FraudRiskResult fraudRiskResult,
                                     RepaymentCapacityResult repaymentResult,
                                     PolicyReviewResult ruleReviewResult) {
        if (!isAvailable()) {
            return withFallbackSource(ruleReviewResult, "未配置 DeepSeek API Key，使用本地规则审查");
        }

        try {
            String prompt = buildPrompt(context, fraudRiskResult, repaymentResult, ruleReviewResult);
            Response<AiMessage> response = chatLanguageModel.generate(List.of(
                    SystemMessage.from("你是信贷审批中的风险复核 Agent。你只能提供辅助审查意见，不能绕过本地硬规则。必须返回合法 JSON，不要返回 Markdown。"),
                    UserMessage.from(prompt)
            ));
            String content = response == null || response.content() == null ? null : response.content().text();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("DeepSeek 返回为空");
            }

            PolicyReviewResult llmReview = objectMapper.readValue(stripCodeFence(content), PolicyReviewResult.class);
            return guardAgainstRuleRelaxation(llmReview, ruleReviewResult);
        } catch (Exception ex) {
            return withFallbackSource(
                    ruleReviewResult,
                    "DeepSeek 调用失败，使用本地规则兜底：" + ex.getClass().getSimpleName()
            );
        }
    }

    private boolean isAvailable() {
        return enabled
                && apiKey != null
                && !apiKey.isBlank()
                && !"disabled".equalsIgnoreCase(apiKey.trim());
    }

    private String buildPrompt(ApprovalCaseContext context,
                               FraudRiskResult fraudRiskResult,
                               RepaymentCapacityResult repaymentResult,
                               PolicyReviewResult ruleReviewResult) throws Exception {
        Map<String, Object> metrics = Map.of(
                "riskLevel", fraudRiskResult.getRiskLevel(),
                "riskScore", fraudRiskResult.getRiskScore(),
                "suggestedAction", fraudRiskResult.getSuggestedAction(),
                "dti", repaymentResult.getDti(),
                "foir", repaymentResult.getFoir(),
                "recommendedCreditLimit", repaymentResult.getRecommendedCreditLimit(),
                "ruleReview", ruleReviewResult
        );

        return """
                请复核以下信贷审批案件。输入只包含脱敏后的结构化结论，不要猜测缺失事实。
                重点检查：风险 Agent 与偿债能力 Agent 是否冲突、是否存在需要人工确认的边界风险、证据是否不足。
                本地规则结论优先级最高：如果本地规则要求人工复核，不得改成 PASS；如果本地规则已经硬性拒绝，不得改成 APPROVED。

                共享案件上下文：
                %s

                风险和偿债指标：
                %s

                只返回以下 JSON 结构，字段名必须保持一致，recommendedAction 只能是 PASS、MANUAL_REVIEW 或 REJECT：
                {
                  "conflictDetected": false,
                  "needManualReview": false,
                  "recommendedAction": "PASS",
                  "confidence": 0.0,
                  "evidence": [""],
                  "summary": ""
                }
                """.formatted(
                objectMapper.writeValueAsString(context.getFindings()),
                objectMapper.writeValueAsString(metrics)
        );
    }

    private PolicyReviewResult guardAgainstRuleRelaxation(PolicyReviewResult llmReview,
                                                           PolicyReviewResult ruleReviewResult) {
        String llmAction = normalizeAction(llmReview.getRecommendedAction());
        String finalAction;
        if ("REJECT".equals(ruleReviewResult.getRecommendedAction())) {
            finalAction = "REJECT";
        } else if (Boolean.TRUE.equals(ruleReviewResult.getNeedManualReview())
                || "REJECT".equals(llmAction)
                || "MANUAL_REVIEW".equals(llmAction)
                || Boolean.TRUE.equals(llmReview.getNeedManualReview())) {
            finalAction = "MANUAL_REVIEW";
        } else {
            finalAction = "PASS";
        }

        List<String> evidence = new ArrayList<>();
        if (llmReview.getEvidence() != null) {
            evidence.addAll(llmReview.getEvidence());
        }
        evidence.add("本地规则审查结论：" + ruleReviewResult.getRecommendedAction());

        String summary = llmReview.getSummary();
        if (summary == null || summary.isBlank()) {
            summary = "DeepSeek 已完成结构化风险复核";
        }
        if (!finalAction.equals(llmAction)) {
            summary += "；已按本地规则安全策略调整为：" + finalAction;
        }

        return new PolicyReviewResult(
                Boolean.TRUE.equals(llmReview.getConflictDetected())
                        || Boolean.TRUE.equals(ruleReviewResult.getConflictDetected()),
                "MANUAL_REVIEW".equals(finalAction),
                finalAction,
                clampConfidence(llmReview.getConfidence()),
                evidence,
                summary,
                SOURCE + ":" + modelName
        );
    }

    private PolicyReviewResult withFallbackSource(PolicyReviewResult ruleReviewResult, String reason) {
        List<String> evidence = new ArrayList<>();
        if (ruleReviewResult.getEvidence() != null) {
            evidence.addAll(ruleReviewResult.getEvidence());
        }
        evidence.add(reason);
        return new PolicyReviewResult(
                ruleReviewResult.getConflictDetected(),
                ruleReviewResult.getNeedManualReview(),
                ruleReviewResult.getRecommendedAction(),
                ruleReviewResult.getConfidence(),
                evidence,
                ruleReviewResult.getSummary() + "；" + reason,
                FALLBACK_SOURCE
        );
    }

    private String normalizeAction(String action) {
        if (action == null) {
            return "MANUAL_REVIEW";
        }
        String normalized = action.trim().toUpperCase();
        return switch (normalized) {
            case "PASS", "MANUAL_REVIEW", "REJECT" -> normalized;
            default -> "MANUAL_REVIEW";
        };
    }

    private BigDecimal clampConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return new BigDecimal("0.50");
        }
        return confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String stripCodeFence(String content) {
        String normalized = content.trim();
        if (normalized.startsWith("```") && normalized.endsWith("```")) {
            int firstLineBreak = normalized.indexOf('\n');
            if (firstLineBreak >= 0) {
                return normalized.substring(firstLineBreak + 1, normalized.length() - 3).trim();
            }
        }
        return normalized;
    }
}
