package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.knowledge.PolicyChunk;
import com.erbu.financialcrisis.knowledge.PolicyKnowledgeStore;
import com.erbu.financialcrisis.domain.entity.PolicyDocument;
import com.erbu.financialcrisis.service.PolicyIngestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** 管理端政策分片导入入口；生产环境应在网关层限制为管理员访问。 */
@RestController
@RequestMapping("/api/admin/policy-knowledge")
public class PolicyKnowledgeController {
    private final PolicyKnowledgeStore store;
    private final PolicyIngestionService ingestionService;

    public PolicyKnowledgeController(PolicyKnowledgeStore store,
                                     PolicyIngestionService ingestionService) {
        this.store = store;
        this.ingestionService = ingestionService;
    }

    /** 上传完整政策文件，后端自动提取文字、切片、向量化并同步到 Qdrant。 */
    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public Result<PolicyIngestionService.PolicyImportResult> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank String documentId,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String version,
            @RequestParam @NotBlank String productCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
            @RequestParam(defaultValue = "admin") String createdBy) {
        return Result.success(ingestionService.importDocument(file, documentId, title, version,
                productCode, effectiveFrom, effectiveTo, createdBy));
    }

    @GetMapping("/documents")
    public Result<List<PolicyDocument>> listDocuments() {
        return Result.success(ingestionService.listDocuments());
    }

    @PostMapping("/chunks")
    public Result<Void> upsert(@Valid @RequestBody UpsertPolicyChunkRequest request) {
        store.upsert(new PolicyChunk(
                request.documentId(), request.chunkId(), request.title(), request.section(),
                request.version(), request.productCode(), request.status(),
                request.effectiveFrom(), request.effectiveTo(), request.content()));
        return Result.success(null);
    }

    public record UpsertPolicyChunkRequest(
            @NotBlank String documentId,
            @NotBlank String chunkId,
            @NotBlank String title,
            String section,
            @NotBlank String version,
            @NotBlank String productCode,
            String status,
            @NotBlank String effectiveFrom,
            String effectiveTo,
            @NotBlank String content
    ) {}
}
