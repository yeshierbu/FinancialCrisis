package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.ApiResponse;
import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端申请入口。
 * 第一版先把主链路接口定义出来，具体文件上传和报告下载可以后续继续扩展。
 */
@RestController
@RequestMapping("/api/loan/applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping
    public ApiResponse<LoanApplicationResponse> createApplication(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return ApiResponse.success(loanApplicationService.createApplication(request));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<LoanApplicationResponse> getApplication(@PathVariable Long applicationId) {
        return ApiResponse.success(loanApplicationService.getApplication(applicationId));
    }
}
