package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.request.UploadDocumentRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;

/**
 * 申请材料服务接口。
 */
public interface DocumentService {

    UploadedDocumentResponse uploadDocument(Long applicationId, UploadDocumentRequest request);

    SupplementResponse submitSupplement(Long applicationId, SupplementRequest request);
}
