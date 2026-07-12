package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端申请入口。
 */
@RestController
@RequestMapping("/api/loan/applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    /** 查询全部贷款申请。 */
    @GetMapping
    public Result<List<LoanApplicationResponse>> listApplications() {
        return Result.success(loanApplicationService.listApplications());
    }

    /** 创建贷款申请并启动自动审批流程。 */
    @PostMapping
    public Result<LoanApplicationResponse> createApplication(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return Result.success(loanApplicationService.createApplication(request));
    }

    /** 根据申请 ID 查询申请详情。 */
    @GetMapping("/{applicationId}")
    public Result<LoanApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return Result.success(loanApplicationService.getApplication(applicationId));
    }

    /** 查询申请当前状态及状态流转时间线。 */
    @GetMapping("/{applicationId}/status")
    public Result<ApplicationStatusResponse> getApplicationStatus(@PathVariable Long applicationId) {
        return Result.success(loanApplicationService.getApplicationStatus(applicationId));
    }
}
