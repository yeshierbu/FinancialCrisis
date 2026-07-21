package com.erbu.financialcrisis.knowledge;

import java.time.LocalDate;
import java.util.List;

/**
 * 政策知识库边界。Agent 只依赖此接口，不感知底层是 Qdrant 还是其他向量库。
 */
public interface PolicyKnowledgeStore {
    /**
     * 按查询文本、产品编码和生效日期检索最相关的政策依据。
     *
     * @param query 用户问题或待匹配的审批上下文
     * @param productCode 产品编码，用于限定政策适用范围
     * @param effectiveDate 生效日期，用于过滤查询时点有效的政策版本
     * @param limit 最大返回条数
     * @return 按相关性排序的政策依据列表
     */
    List<PolicyEvidence> search(String query, String productCode, LocalDate effectiveDate, int limit);

    /**
     * 新增或更新一段政策知识切片。
     *
     * <p>实现类应保证同一政策切片重复写入时具备幂等性，避免产生重复向量或重复记录。</p>
     *
     * @param chunk 待写入的政策知识切片
     */
    void upsert(PolicyChunk chunk);

    /**
     * 删除指定政策文档版本下的全部知识切片。
     *
     * @param documentId 政策文档 ID
     * @param version 政策文档版本号
     */
    void deleteDocumentVersion(String documentId, String version);
}
