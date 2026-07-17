package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审计时间线响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTimelineResponse {

    private Long applicationId;
    private String applicationNo;
    private ApplicationStatus status;
    private List<AuditTimelineItemResponse> timeline;
}
