package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批进度响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusResponse {

    private Long applicationId;
    private ApplicationStatus status;
    private String statusDesc;
    private LocalDateTime lastUpdatedAt;
    private List<StatusTimelineResponse> timeline;
}
