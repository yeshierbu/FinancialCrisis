package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.dto.response.ApprovalReportResponse;
import com.erbu.financialcrisis.service.ApprovalReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端审批报告入口。
 */
@RestController
@RequestMapping("/api/loan/applications")
public class ApprovalReportController {

    private final ApprovalReportService approvalReportService;

    public ApprovalReportController(ApprovalReportService approvalReportService) {
        this.approvalReportService = approvalReportService;
    }

    /** 查询已结束申请的审批报告。 */
    @GetMapping("/{applicationId}/report")
    public Result<ApprovalReportResponse> getReport(@PathVariable Long applicationId) {
        return Result.success(approvalReportService.getReport(applicationId));
    }
}
