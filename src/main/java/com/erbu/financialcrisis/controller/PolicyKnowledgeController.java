package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.knowledge.PolicyChunk;
import com.erbu.financialcrisis.knowledge.PolicyKnowledgeStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端政策分片导入入口；生产环境应在网关层限制为管理员访问。 */
@RestController
@RequestMapping("/api/admin/policy-knowledge")
public class PolicyKnowledgeController {
    private final PolicyKnowledgeStore store;

    public PolicyKnowledgeController(PolicyKnowledgeStore store) {
        this.store = store;
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
