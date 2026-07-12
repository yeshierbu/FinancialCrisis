package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;
import com.erbu.financialcrisis.domain.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

/**
 * 申请材料服务接口。
 */
public interface DocumentService {

    /** 保存一份申请材料并重新推进审批。 */
    UploadedDocumentResponse uploadDocument(Long applicationId, DocumentType documentType, MultipartFile file);

    /** 提交多份补充材料并重新推进审批。 */
    SupplementResponse submitSupplement(Long applicationId, SupplementRequest request);
}
