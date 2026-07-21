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
    /** 访问 Qdrant HTTP API 的客户端。 */
    private final RestClient restClient;

    /** 用于把查询文本或政策正文转换成向量。 */
    private final EmbeddingClient embeddingClient;

    /** Qdrant Collection 名称，存放政策知识切片向量和 payload。 */
    private final String collection;

    /** Qdrant API Key，未配置时按本地无鉴权模式访问。 */
    private final String apiKey;

    /** 知识库功能总开关，关闭时不访问 Qdrant。 */
    private final boolean enabled;

    /**
     * 初始化 Qdrant 政策知识库实现。
     *
     * <p>所有配置均支持通过配置文件覆盖；默认连接本地 Qdrant，并使用
     * {@code credit_policy_chunks} 作为政策切片集合。</p>
     */
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

    /**
     * 基于查询文本做向量检索，并用 payload 精确过滤政策适用范围。
     *
     * <p>检索时先将 {@code query} 转成向量，再在 Qdrant 中限定状态为 ACTIVE；
     * 如果传入产品编码，则额外限定产品编码。返回结果会在应用侧再次校验生效日期。</p>
     */
    /**
     *
     * @param query 用户问题或待匹配的审批上下文
     * @param productCode 产品编码，用于限定政策适用范围
     * @param effectiveDate 生效日期，用于过滤查询时点有效的政策版本
     * @param limit 最大返回条数
     * @return
     */
    @Override
    public List<PolicyEvidence> search(String query, String productCode,
                                       LocalDate effectiveDate, int limit) {
        if (!enabled || !embeddingClient.isAvailable()) {
            return List.of();
        }
        //向量化用户问题并进行过滤缩锁定查询范围
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
        //向向量数据库发送请求
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

    /**
     * 写入或更新一条政策知识切片。
     *
     * <p>写入前会先根据正文生成向量，并确保 Qdrant Collection 已存在。
     * point id 由 {@code chunkId} 稳定生成，因此同一切片重复导入会覆盖旧记录。</p>
     */
    @Override
    public void upsert(PolicyChunk chunk) {
        if (!enabled || !embeddingClient.isAvailable()) {
            throw new IllegalStateException("政策知识库未启用或 Embedding 服务未配置");
        }
        List<Double> vector = embeddingClient.embed(chunk.getContent());
        // Qdrant 集合的幂等初始化——存在就跳过，不存在就按指定维度和余弦距离创建。保证应用启动后集合一定可用，不需要人工预先建库
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

    /**
     * 删除指定政策文档版本对应的全部切片。
     *
     * <p>删除条件通过 payload 中的 {@code documentId} 和 {@code version} 精确匹配；
     * 如果 Collection 尚不存在，说明还没有可删除的数据，直接忽略。</p>
     */
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

    /**
     * 确保目标 Collection 存在。
     *
     * <p>如果 Collection 不存在，则按当前 Embedding 向量维度创建，并使用 Cosine 距离
     * 作为向量相似度计算方式。</p>
     */
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

    /** 如果配置了 Qdrant API Key，则为请求追加鉴权头。 */
    private void addApiKey(RestClient.RequestBodySpec request) {
        if (apiKey != null && !apiKey.isBlank()) request.header("api-key", apiKey);
    }

    /** 构造 Qdrant payload 精确匹配过滤条件。 */
    private Map<String, Object> match(String key, String value) {
        return Map.of("key", key, "match", Map.of("value", value));
    }

    /** 判断政策切片在指定日期是否有效。 */
    private boolean isEffective(JsonNode payload, LocalDate date) {
        if (date == null) return true;
        String from = text(payload, "effectiveFrom");
        String to = text(payload, "effectiveTo");
        return (from == null || !LocalDate.parse(from).isAfter(date))
                && (to == null || !LocalDate.parse(to).isBefore(date));
    }

    /** 从 JSON payload 中读取非空字符串字段。 */
    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }
}
