package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.response.ApprovalReportResponse;

/**
 * 审批报告服务接口。
 */
public interface ApprovalReportService {

    ApprovalReportResponse getReport(Long applicationId);
}
