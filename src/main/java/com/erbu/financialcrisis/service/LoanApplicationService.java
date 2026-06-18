package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;

/**
 * 申请服务接口。
 */
public interface LoanApplicationService {

    LoanApplicationResponse createApplication(CreateLoanApplicationRequest request);

    LoanApplicationResponse getApplication(Long applicationId);
}
