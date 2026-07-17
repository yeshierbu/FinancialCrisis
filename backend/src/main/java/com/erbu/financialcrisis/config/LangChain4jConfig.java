package com.erbu.financialcrisis.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 基础配置。
 * 使用 DeepSeek 的 OpenAI 兼容接口。请求和响应日志默认关闭，避免把申请数据写入日志。
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(@Value("${llm.api-key:}") String apiKey,
                                               @Value("${llm.base-url:https://api.deepseek.com}") String baseUrl,
                                               @Value("${llm.model:deepseek-v4-flash}") String modelName,
                                               @Value("${llm.timeout-seconds:45}") long timeoutSeconds) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.1)
                .maxTokens(800)
                .responseFormat("json_object")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(1)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
