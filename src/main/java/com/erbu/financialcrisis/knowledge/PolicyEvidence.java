package com.erbu.financialcrisis.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Qdrant 返回的一段可引用政策证据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvidence {
    private String documentId;
    private String chunkId;
    private String title;
    private String section;
    private String version;
    private String content;
    private Double score;
}
