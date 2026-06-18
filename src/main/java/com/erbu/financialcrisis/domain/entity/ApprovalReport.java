package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批报告表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalReport {

    private Long reportId;
    private Long applicationId;
    private ReportType reportType;
    private String reportUrl;
    private String reportVersion;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
