package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.request.UploadDocumentRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;
import com.erbu.financialcrisis.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/{applicationId}/documents")
    public Result<UploadedDocumentResponse> uploadDocument(@PathVariable Long applicationId,
                                                           @Valid @RequestBody UploadDocumentRequest request) {
        return Result.success(documentService.uploadDocument(applicationId, request));
    }

    @PostMapping("/{applicationId}/supplement")
    public Result<SupplementResponse> submitSupplement(@PathVariable Long applicationId,
                                                       @Valid @RequestBody SupplementRequest request) {
        return Result.success(documentService.submitSupplement(applicationId, request));
    }
}
