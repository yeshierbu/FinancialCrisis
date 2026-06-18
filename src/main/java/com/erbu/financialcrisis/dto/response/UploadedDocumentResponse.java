package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.domain.enums.OcrStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传申请材料响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocumentResponse {

    private Long documentId;
    private Long applicationId;
    private DocumentType documentType;
    private OcrStatus ocrStatus;
}
