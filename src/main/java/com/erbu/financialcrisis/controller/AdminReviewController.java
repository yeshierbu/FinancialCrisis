package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.ApiResponse;
import com.erbu.financialcrisis.dto.request.ManualReviewRequest;
import com.erbu.financialcrisis.service.ManualReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端人工复核入口。
 * 自动审批无法完成时，会落到这里由人工做最终判断。
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ManualReviewService manualReviewService;

    public AdminReviewController(ManualReviewService manualReviewService) {
        this.manualReviewService = manualReviewService;
    }

    @PostMapping("/{applicationId}/approve")
    public ApiResponse<String> approve(@PathVariable Long applicationId,
                                       @Valid @RequestBody ManualReviewRequest request) {
        manualReviewService.approve(applicationId, request);
        return ApiResponse.success("人工审批通过");
    }

    @PostMapping("/{applicationId}/reject")
    public ApiResponse<String> reject(@PathVariable Long applicationId,
                                      @Valid @RequestBody ManualReviewRequest request) {
        manualReviewService.reject(applicationId, request);
        return ApiResponse.success("人工审批拒绝");
    }
}
