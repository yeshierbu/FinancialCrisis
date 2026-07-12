package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.DocumentType;
import com.erbu.financialcrisis.domain.enums.OcrStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.dto.request.SupplementDocumentRequest;
import com.erbu.financialcrisis.dto.request.SupplementRequest;
import com.erbu.financialcrisis.dto.response.SupplementResponse;
import com.erbu.financialcrisis.dto.response.UploadedDocumentResponse;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.service.DocumentService;
import com.erbu.financialcrisis.service.QianfanOcrService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 申请材料业务服务实现。
 *
 * <p>接收身份证、银行流水等真实图片，调用百度千帆 DeepSeek-OCR，
 * 仅持久化文件元数据和 OCR 文本，不在本地保存原始图片。</p>
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final ApprovalStore store;
    private final AgentOrchestrationService agentOrchestrationService;
    private final QianfanOcrService qianfanOcrService;

    public DocumentServiceImpl(ApprovalStore store,
                               AgentOrchestrationService agentOrchestrationService,
                               QianfanOcrService qianfanOcrService) {
        this.store = store;
        this.agentOrchestrationService = agentOrchestrationService;
        this.qianfanOcrService = qianfanOcrService;
    }

    @Override
    @Transactional
    public UploadedDocumentResponse uploadDocument(Long applicationId,
                                                   DocumentType documentType,
                                                   MultipartFile file) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        assertDocumentUploadAllowed(application);
        byte[] fileBytes = readAndValidateFile(file);
        String fileName = safeFileName(file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);

        UploadedDocument document = new UploadedDocument(
                null,
                applicationId,
                documentType,
                fileName,
                "transient://ocr-inputs/" + applicationId + "/" + UUID.randomUUID(),
                file.getSize(),
                sha256(fileBytes),
                OcrStatus.PENDING,
                null,
                LocalDateTime.now()
        );
        store.addDocument(document);

        try {
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
            document.setParseResultJson(qianfanOcrService.recognize(documentType, dataUrl));
            document.setOcrStatus(OcrStatus.SUCCESS);
            store.updateDocument(document);
        } catch (RuntimeException ex) {
            document.setOcrStatus(OcrStatus.FAILED);
            document.setParseResultJson("{\"provider\":\"BAIDU_QIANFAN\",\"error\":\"OCR_FAILED\"}");
            store.updateDocument(document);
            store.changeStatus(
                    application,
                    ApplicationStatus.DOCUMENT_PENDING,
                    "百度千帆 OCR 识别失败，请重新上传清晰图片",
                    "OCR_FAILED",
                    OperatorType.SYSTEM,
                    "BaiduQianfanOcrService",
                    ex.getMessage()
            );
            return toResponse(document);
        }

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
                "上传材料：" + documentType
        );
        agentOrchestrationService.startApprovalFlow(applicationId);

        return toResponse(document);
    }

    @Override
    @Transactional
    public SupplementResponse submitSupplement(Long applicationId, SupplementRequest request) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        if (application.getStatus() != ApplicationStatus.DOCUMENT_PENDING
                && application.getStatus() != ApplicationStatus.MATERIAL_PENDING) {
            throw new BusinessException(4003, "当前状态不允许提交补充资料");
        }

        boolean ocrFailed = false;
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
            try {
                document.setParseResultJson(qianfanOcrService.recognize(
                        documentRequest.getDocumentType(), documentRequest.getFileUrl()
                ));
                document.setOcrStatus(OcrStatus.SUCCESS);
            } catch (RuntimeException ex) {
                document.setOcrStatus(OcrStatus.FAILED);
                document.setParseResultJson("{\"provider\":\"BAIDU_QIANFAN\",\"error\":\"OCR_FAILED\"}");
                ocrFailed = true;
            }
            store.updateDocument(document);
        }

        if (ocrFailed) {
            store.changeStatus(
                    application,
                    ApplicationStatus.DOCUMENT_PENDING,
                    "部分补充材料 OCR 识别失败，请重新上传",
                    "OCR_FAILED",
                    OperatorType.SYSTEM,
                    "BaiduQianfanOcrService",
                    "补充材料 OCR 失败"
            );
            return new SupplementResponse(applicationId, application.getStatus(), application.getCurrentStep());
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

    private byte[] readAndValidateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(4004, "上传图片不能为空");
        }
        String fileName = safeFileName(file.getOriginalFilename());
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        boolean extensionAllowed = ALLOWED_EXTENSIONS.stream().anyMatch(lowerFileName::endsWith);
        if (!extensionAllowed) {
            throw new BusinessException(4004, "文件格式不支持，仅允许 jpg、jpeg、png");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(4004, "文件内容类型不支持，仅允许 JPEG 或 PNG 图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(4004, "文件大小不能超过 10MB");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(4004, "读取上传图片失败");
        }
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.jpg";
        }
        String normalized = originalFilename.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }

    private UploadedDocumentResponse toResponse(UploadedDocument document) {
        return new UploadedDocumentResponse(
                document.getDocumentId(),
                document.getApplicationId(),
                document.getDocumentType(),
                document.getOcrStatus()
        );
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
