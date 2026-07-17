package com.erbu.financialcrisis.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 写入 Qdrant 的政策分片及其精确过滤元数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyChunk {
    private String documentId;
    private String chunkId;
    private String title;
    private String section;
    private String version;
    private String productCode;
    private String status;
    private String effectiveFrom;
    private String effectiveTo;
    private String content;
}
