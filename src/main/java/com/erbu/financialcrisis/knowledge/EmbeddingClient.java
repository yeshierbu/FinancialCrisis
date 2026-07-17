package com.erbu.financialcrisis.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** 调用 OpenAI 兼容的 Embedding 接口，为 Qdrant 查询生成向量。 */
@Component
public class EmbeddingClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public EmbeddingClient(RestClient.Builder builder,
                           @Value("${knowledge.embedding.base-url:https://api.openai.com/v1}") String baseUrl,
                           @Value("${knowledge.embedding.api-key:}") String apiKey,
                           @Value("${knowledge.embedding.model:text-embedding-3-small}") String model,
                           @Value("${knowledge.enabled:false}") boolean enabled) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public List<Double> embed(String text) {
        if (!isAvailable()) {
            return List.of();
        }
        JsonNode response = restClient.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of("model", model, "input", text))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.path("data").isEmpty()) {
            throw new IllegalStateException("Embedding 服务返回为空");
        }
        JsonNode vector = response.path("data").get(0).path("embedding");
        if (!vector.isArray()) {
            throw new IllegalStateException("Embedding 服务响应缺少向量数组");
        }
        java.util.ArrayList<Double> values = new java.util.ArrayList<>();
        vector.forEach(value -> values.add(value.asDouble()));
        return List.copyOf(values);
    }
}
