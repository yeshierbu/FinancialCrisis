package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.ApprovalReport;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.ReportType;
import com.erbu.financialcrisis.dto.response.ApprovalReportResponse;
import com.erbu.financialcrisis.service.ApprovalReportService;
import com.erbu.financialcrisis.store.InMemoryApprovalStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审批报告业务服务实现。
 *
 * <p>第一版只生成 JSON 报告地址，不做真实 PDF。这样可以先完成报告查询接口和报告表实体映射；
 * 后续如果要导出 PDF，可以在这里接模板引擎、HTML 转 PDF 和对象存储。</p>
 */
@Service
public class ApprovalReportServiceImpl implements ApprovalReportService {

    private final InMemoryApprovalStore store;

    public ApprovalReportServiceImpl(InMemoryApprovalStore store) {
        this.store = store;
    }

    @Override
    public ApprovalReportResponse getReport(Long applicationId) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        if (application.getStatus() != ApplicationStatus.APPROVED
                && application.getStatus() != ApplicationStatus.REJECTED) {
            throw new BusinessException(4003, "只有审批通过或拒绝后才能生成审批报告");
        }

        ApprovalReport report = store.findApprovalReport(applicationId).orElseGet(() -> createReport(application));
        store.saveApprovalReport(report);
        return new ApprovalReportResponse(
                report.getReportId(),
                report.getApplicationId(),
                report.getReportType(),
                report.getReportUrl(),
                report.getGeneratedAt()
        );
    }

    private ApprovalReport createReport(LoanApplication application) {
        LocalDateTime now = LocalDateTime.now();
        return new ApprovalReport(
                store.nextReportId(),
                application.getApplicationId(),
                ReportType.INTERNAL_AUDIT,
                "memory://approval-reports/" + application.getApplicationNo() + "/v1.json",
                "v1",
                "report-service",
                now,
                now,
                now
        );
    }
}
