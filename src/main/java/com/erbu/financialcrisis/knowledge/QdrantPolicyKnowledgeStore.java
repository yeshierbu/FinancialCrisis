package com.erbu.financialcrisis.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Qdrant 政策检索实现。
 *
 * <p>向量只用于找“语义相关”的政策段落；产品、状态和生效日期仍使用 payload
 * 精确过滤，避免召回已失效或不属于当前产品的政策。</p>
 */
@Repository
public class QdrantPolicyKnowledgeStore implements PolicyKnowledgeStore {
    private final RestClient restClient;
    private final EmbeddingClient embeddingClient;
    private final String collection;
    private final String apiKey;
    private final boolean enabled;

    public QdrantPolicyKnowledgeStore(RestClient.Builder builder,
                                      EmbeddingClient embeddingClient,
                                      @Value("${knowledge.qdrant.url:http://localhost:6333}") String url,
                                      @Value("${knowledge.qdrant.collection:credit_policy_chunks}") String collection,
                                      @Value("${knowledge.qdrant.api-key:}") String apiKey,
                                      @Value("${knowledge.enabled:false}") boolean enabled) {
        this.restClient = builder.baseUrl(url).build();
        this.embeddingClient = embeddingClient;
        this.collection = collection;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    @Override
    public List<PolicyEvidence> search(String query, String productCode,
                                       LocalDate effectiveDate, int limit) {
        if (!enabled || !embeddingClient.isAvailable()) {
            return List.of();
        }
        List<Double> vector = embeddingClient.embed(query);
        List<Map<String, Object>> must = new ArrayList<>();
        must.add(match("status", "ACTIVE"));
        if (productCode != null && !productCode.isBlank()) {
            must.add(match("productCode", productCode));
        }

        Map<String, Object> body = Map.of(
                "vector", vector,
                "limit", Math.max(1, Math.min(limit, 10)),
                "with_payload", true,
                "filter", Map.of("must", must)
        );
        RestClient.RequestBodySpec request = restClient.post()
                .uri("/collections/{collection}/points/search", collection)
                .contentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            request.header("api-key", apiKey);
        }
        JsonNode response = request.body(body).retrieve().body(JsonNode.class);
        if (response == null || !response.path("result").isArray()) {
            return List.of();
        }

        List<PolicyEvidence> evidence = new ArrayList<>();
        for (JsonNode hit : response.path("result")) {
            JsonNode payload = hit.path("payload");
            // 日期再在应用侧校验一次，防止知识库 payload 配置错误。
            if (!isEffective(payload, effectiveDate)) {
                continue;
            }
            evidence.add(new PolicyEvidence(
                    text(payload, "documentId"), text(payload, "chunkId"),
                    text(payload, "title"), text(payload, "section"),
                    text(payload, "version"), text(payload, "content"),
                    hit.path("score").asDouble()
            ));
        }
        return evidence;
    }

    @Override
    public void upsert(PolicyChunk chunk) {
        if (!enabled || !embeddingClient.isAvailable()) {
            throw new IllegalStateException("政策知识库未启用或 Embedding 服务未配置");
        }
        List<Double> vector = embeddingClient.embed(chunk.getContent());
        ensureCollection(vector.size());
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("documentId", chunk.getDocumentId());
        payload.put("chunkId", chunk.getChunkId());
        payload.put("title", chunk.getTitle());
        payload.put("section", chunk.getSection());
        payload.put("version", chunk.getVersion());
        payload.put("productCode", chunk.getProductCode());
        payload.put("status", chunk.getStatus() == null ? "ACTIVE" : chunk.getStatus());
        payload.put("effectiveFrom", chunk.getEffectiveFrom());
        payload.put("effectiveTo", chunk.getEffectiveTo());
        payload.put("content", chunk.getContent());
        payload.values().removeIf(java.util.Objects::isNull);

        // UUID 由稳定的 chunkId 生成，重复导入同一分片会覆盖而不是产生重复向量。
        String pointId = java.util.UUID.nameUUIDFromBytes(
                chunk.getChunkId().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        RestClient.RequestBodySpec request = restClient.put()
                .uri("/collections/{collection}/points?wait=true", collection)
                .contentType(MediaType.APPLICATION_JSON);
        addApiKey(request);
        request.body(Map.of("points", List.of(Map.of(
                        "id", pointId, "vector", vector, "payload", payload))))
                .retrieve().toBodilessEntity();
    }

    @Override
    public void deleteDocumentVersion(String documentId, String version) {
        if (!enabled) return;
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri("/collections/{collection}/points/delete?wait=true", collection)
                    .contentType(MediaType.APPLICATION_JSON);
            addApiKey(request);
            request.body(Map.of("filter", Map.of("must", List.of(
                            match("documentId", documentId), match("version", version)))))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // 首次导入时 Collection 尚未创建，由第一条 upsert 根据向量维度自动创建。
        }
    }

    private void ensureCollection(int vectorSize) {
        try {
            RestClient.RequestHeadersSpec<?> get = restClient.get()
                    .uri("/collections/{collection}", collection);
            if (apiKey != null && !apiKey.isBlank()) get.header("api-key", apiKey);
            get.retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound notFound) {
            RestClient.RequestBodySpec create = restClient.put()
                    .uri("/collections/{collection}", collection)
                    .contentType(MediaType.APPLICATION_JSON);
            addApiKey(create);
            create.body(Map.of("vectors", Map.of("size", vectorSize, "distance", "Cosine")))
                    .retrieve().toBodilessEntity();
        }
    }

    private void addApiKey(RestClient.RequestBodySpec request) {
        if (apiKey != null && !apiKey.isBlank()) request.header("api-key", apiKey);
    }

    private Map<String, Object> match(String key, String value) {
        return Map.of("key", key, "match", Map.of("value", value));
    }

    private boolean isEffective(JsonNode payload, LocalDate date) {
        if (date == null) return true;
        String from = text(payload, "effectiveFrom");
        String to = text(payload, "effectiveTo");
        return (from == null || !LocalDate.parse(from).isAfter(date))
                && (to == null || !LocalDate.parse(to).isBefore(date));
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }
}
