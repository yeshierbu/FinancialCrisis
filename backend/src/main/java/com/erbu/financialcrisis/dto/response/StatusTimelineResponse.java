package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批进度时间线响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusTimelineResponse {

    private ApplicationStatus status;
    private LocalDateTime time;
}
