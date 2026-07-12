package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.OcrStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.dto.request.SupplementDocumentRequest;
import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.request.UploadDocumentRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.service.DocumentService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 申请材料业务服务实现。
 *
 * <p>第一版不直接接对象存储和 MultipartFile，而是用 fileUrl 表示已经上传好的文件地址。
 * 这样可以先把“材料元数据 -> OCR 状态 -> 审批继续推进”的主链路跑通。</p>
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".pdf");

    private final ApprovalStore store;
    private final AgentOrchestrationService agentOrchestrationService;

    public DocumentServiceImpl(ApprovalStore store,
                               AgentOrchestrationService agentOrchestrationService) {
        this.store = store;
        this.agentOrchestrationService = agentOrchestrationService;
    }

    @Override
    @Transactional
    public UploadedDocumentResponse uploadDocument(Long applicationId, UploadDocumentRequest request) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        assertDocumentUploadAllowed(application);
        validateFile(request.getFileName(), request.getFileSize());

        UploadedDocument document = new UploadedDocument(
                null,
                applicationId,
                request.getDocumentType(),
                request.getFileName(),
                request.getFileUrl(),
                request.getFileSize(),
                request.getFileHash(),
                OcrStatus.PENDING,
                null,
                LocalDateTime.now()
        );
        store.addDocument(document);

        /*
         * 文件上传后立即重新拉起审批流。资料不齐时流程会再次停在 DOCUMENT_PENDING；
         * 资料齐全时则会继续进入风控、偿债能力和合规决策。
         */
        store.changeStatus(
                application,
                ApplicationStatus.SUBMITTED,
                "材料已接收，准备重新进入自动审批",
                "UPLOAD_DOCUMENT",
                OperatorType.USER,
                application.getApplicantName(),
                "上传材料：" + request.getDocumentType()
        );
        agentOrchestrationService.startApprovalFlow(applicationId);

        return new UploadedDocumentResponse(
                document.getDocumentId(),
                document.getApplicationId(),
                document.getDocumentType(),
                document.getOcrStatus()
        );
    }

    @Override
    @Transactional
    public SupplementResponse submitSupplement(Long applicationId, SupplementRequest request) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        if (application.getStatus() != ApplicationStatus.DOCUMENT_PENDING
                && application.getStatus() != ApplicationStatus.MATERIAL_PENDING) {
            throw new BusinessException(4003, "当前状态不允许提交补充资料");
        }

        for (SupplementDocumentRequest documentRequest : request.getDocuments()) {
            String documentKey = UUID.randomUUID().toString().replace("-", "");
            UploadedDocument document = new UploadedDocument(
                    null,
                    applicationId,
                    documentRequest.getDocumentType(),
                    buildSupplementFileName(documentKey, documentRequest),
                    documentRequest.getFileUrl(),
                    null,
                    "SUP-" + documentKey,
                    OcrStatus.PENDING,
                    null,
                    LocalDateTime.now()
            );
            store.addDocument(document);
        }

        store.changeStatus(
                application,
                ApplicationStatus.SUBMITTED,
                "补充资料已提交，重新进入自动审批",
                "SUBMIT_SUPPLEMENT",
                OperatorType.USER,
                application.getApplicantName(),
                request.getRemark()
        );
        agentOrchestrationService.startApprovalFlow(applicationId);

        LoanApplication latest = store.getApplicationOrThrow(applicationId);
        return new SupplementResponse(latest.getApplicationId(), latest.getStatus(), latest.getCurrentStep());
    }

    private void assertDocumentUploadAllowed(LoanApplication application) {
        if (application.getStatus() == ApplicationStatus.APPROVED
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.ARCHIVED
                || application.getStatus() == ApplicationStatus.MANUAL_REVIEW) {
            throw new BusinessException(4003, "当前状态不允许上传材料");
        }
    }

    private void validateFile(String fileName, Long fileSize) {
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        boolean extensionAllowed = ALLOWED_EXTENSIONS.stream().anyMatch(lowerFileName::endsWith);
        if (!extensionAllowed) {
            throw new BusinessException(4004, "文件格式不支持，仅允许 jpg、jpeg、png、pdf");
        }
        if (fileSize != null && fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(4004, "文件大小不能超过 20MB");
        }
    }

    private String buildSupplementFileName(String documentKey, SupplementDocumentRequest request) {
        String fileUrl = request.getFileUrl();
        int slashIndex = fileUrl.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(slashIndex + 1);
        }
        return request.getDocumentType() + "-supplement-" + documentKey + ".pdf";
    }
}
