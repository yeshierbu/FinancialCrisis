package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    public Result<LoanApplicationResponse> createApplication(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return Result.success(loanApplicationService.createApplication(request));
    }

    @PostMapping("/{applicationId}")
    public Result<LoanApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return Result.success(loanApplicationService.getApplication(applicationId));
    }

    @PostMapping("/{applicationId}/status")
    public Result<ApplicationStatusResponse> getApplicationStatus(@PathVariable Long applicationId) {
        return Result.success(loanApplicationService.getApplicationStatus(applicationId));
    }
}
