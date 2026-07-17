package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.response.ApprovalReportResponse;

/**
 * 审批报告服务接口。
 */
public interface ApprovalReportService {

    /** 查询或生成已结束申请的审批报告记录。 */
    ApprovalReportResponse getReport(Long applicationId);
}
