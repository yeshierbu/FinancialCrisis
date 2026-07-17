package com.erbu.financialcrisis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** MySQL 中的政策主数据；正文分片向量保存在 Qdrant。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDocument {
    private Long id;
    private String documentId;
    private String title;
    private String version;
    private String productCode;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String sourceUrl;
    private String contentHash;
    private String qdrantCollection;
    private String vectorSyncStatus;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
