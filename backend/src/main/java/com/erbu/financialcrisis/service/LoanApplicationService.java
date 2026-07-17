package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;

import java.util.List;

/**
 * 贷款申请服务接口。
 */
public interface LoanApplicationService {

    /** 创建申请并启动审批。 */
    LoanApplicationResponse createApplication(CreateLoanApplicationRequest request);

    /** 查询全部申请。 */
    List<LoanApplicationResponse> listApplications();

    /** 查询单个申请详情。 */
    LoanApplicationResponse getApplication(Long applicationId);

    /** 查询申请状态及流转记录。 */
    ApplicationStatusResponse getApplicationStatus(Long applicationId);
}
