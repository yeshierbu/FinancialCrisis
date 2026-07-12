package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;
import com.erbu.financialcrisis.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端申请材料入口。
 */
@RestController
@RequestMapping("/api/loan/applications")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** 接收真实图片、调用百度千帆 OCR，并重新触发审批流程。 */
    @PostMapping(value = "/{applicationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadedDocumentResponse> uploadDocument(@PathVariable Long applicationId,
                                                           @RequestParam("documentType") DocumentType documentType,
                                                           @RequestPart("file") MultipartFile file) {
        return Result.success(documentService.uploadDocument(applicationId, documentType, file));
    }

    /** 批量提交补充材料，并继续执行审批流程。 */
    @PostMapping("/{applicationId}/supplement")
    public Result<SupplementResponse> submitSupplement(@PathVariable Long applicationId,
                                                       @Valid @RequestBody SupplementRequest request) {
        return Result.success(documentService.submitSupplement(applicationId, request));
    }
}
