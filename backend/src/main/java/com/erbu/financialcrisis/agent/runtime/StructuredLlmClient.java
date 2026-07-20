package com.erbu.financialcrisis.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** LLM Worker 共用的结构化模型调用入口，统一处理可用性和 JSON 校验。 */
@Component
public class StructuredLlmClient {
    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;    //序列化/反序列化类
    private final String apiKey;
    private final boolean enabled;

    public StructuredLlmClient(ChatLanguageModel model, ObjectMapper objectMapper,
                               @Value("${llm.api-key:}") String apiKey,
                               @Value("${llm.enabled:true}") boolean enabled) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    public <T> T generate(String systemPrompt, String userPrompt, Class<T> resultType) {
        if (!enabled || apiKey == null || apiKey.isBlank() || "disabled".equalsIgnoreCase(apiKey)) {
            throw new IllegalStateException("审批 LLM 未启用或未配置 API Key");
        }
        try {
            Response<AiMessage> response = model.generate(List.of(
                    SystemMessage.from(systemPrompt + " 必须只返回合法 JSON，不要使用 Markdown。"),
                    UserMessage.from(userPrompt)
            ));


            String content = response == null || response.content() == null
                    ? null : response.content().text();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("模型返回为空");
            }
            return objectMapper.readValue(stripCodeFence(content), resultType);
        } catch (Exception ex) {
            throw new IllegalStateException("LLM 结构化调用失败", ex);
        }
    }

    private String stripCodeFence(String content) {
        String value = content.trim();
        if (!value.startsWith("```") || !value.endsWith("```")) return value;
        int firstBreak = value.indexOf('\n');
        return firstBreak < 0 ? value : value.substring(firstBreak + 1, value.length() - 3).trim();
    }
}
