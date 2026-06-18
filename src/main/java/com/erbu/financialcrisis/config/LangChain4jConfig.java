package com.erbu.financialcrisis.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 基础配置。
 * 这里先提供最小可运行的模型 Bean，后续可以再扩展成多模型、多环境配置。
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(@Value("${llm.api-key:demo-key}") String apiKey,
                                               @Value("${llm.base-url:https://api.openai.com/v1}") String baseUrl,
                                               @Value("${llm.model:gpt-4o-mini}") String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
