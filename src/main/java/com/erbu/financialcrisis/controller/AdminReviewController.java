package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.dto.request.ManualReviewRequest;
import com.erbu.financialcrisis.dto.request.ManualReviewPendingQueryRequest;
import com.erbu.financialcrisis.dto.response.ManualReviewDetailResponse;
import com.erbu.financialcrisis.dto.response.ManualReviewPendingResponse;
import com.erbu.financialcrisis.dto.response.ManualReviewResponse;
import com.erbu.financialcrisis.service.ManualReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端人工复核入口。
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ManualReviewService manualReviewService;

    public AdminReviewController(ManualReviewService manualReviewService) {
        this.manualReviewService = manualReviewService;
    }

    @PostMapping("/pending")
    public Result<List<ManualReviewPendingResponse>> queryPendingReviews(@RequestBody ManualReviewPendingQueryRequest request) {
        return Result.success(manualReviewService.queryPendingReviews(request));
    }

    @PostMapping("/{applicationId}")
    public Result<ManualReviewDetailResponse> getReviewDetail(@PathVariable Long applicationId) {
        return Result.success(manualReviewService.getReviewDetail(applicationId));
    }

    @PostMapping("/{applicationId}/approve")
    public Result<ManualReviewResponse> approve(@PathVariable Long applicationId,
                                                @Valid @RequestBody ManualReviewRequest request) {
        return Result.success(manualReviewService.approve(applicationId, request));
    }

    @PostMapping("/{applicationId}/reject")
    public Result<ManualReviewResponse> reject(@PathVariable Long applicationId,
                                               @Valid @RequestBody ManualReviewRequest request) {
        return Result.success(manualReviewService.reject(applicationId, request));
    }
}
