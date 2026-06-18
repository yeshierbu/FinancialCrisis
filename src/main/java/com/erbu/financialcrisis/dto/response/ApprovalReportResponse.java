package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批报告响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalReportResponse {

    private Long reportId;
    private Long applicationId;
    private ReportType reportType;
    private String reportUrl;
    private LocalDateTime generatedAt;
}
