package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.domain.enums.OcrStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 上传材料表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {

    private Long documentId;
    private Long applicationId;
    private DocumentType documentType;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileHash;
    private OcrStatus ocrStatus;
    private String parseResultJson;
    private LocalDateTime uploadedAt;
}
