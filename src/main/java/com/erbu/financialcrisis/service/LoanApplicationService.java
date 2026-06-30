package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;

import java.util.List;

/**
 * 贷款申请服务接口。
 */
public interface LoanApplicationService {

    LoanApplicationResponse createApplication(CreateLoanApplicationRequest request);

    List<LoanApplicationResponse> listApplications();

    LoanApplicationResponse getApplication(Long applicationId);

    ApplicationStatusResponse getApplicationStatus(Long applicationId);
}
