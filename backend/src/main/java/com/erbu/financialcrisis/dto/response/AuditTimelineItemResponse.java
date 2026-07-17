package com.erbu.financialcrisis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计时间线明细响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTimelineItemResponse {

    private String eventType;
    private String eventName;
    private String operatorName;
    private String summary;
    private LocalDateTime occurredAt;
}
