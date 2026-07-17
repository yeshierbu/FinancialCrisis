package com.erbu.financialcrisis.knowledge;

import java.time.LocalDate;
import java.util.List;

/**
 * 政策知识库边界。Agent 只依赖此接口，不感知底层是 Qdrant 还是其他向量库。
 */
public interface PolicyKnowledgeStore {
    List<PolicyEvidence> search(String query, String productCode, LocalDate effectiveDate, int limit);

    void upsert(PolicyChunk chunk);
}
