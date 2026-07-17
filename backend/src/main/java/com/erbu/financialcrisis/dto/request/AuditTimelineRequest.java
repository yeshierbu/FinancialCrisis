package com.erbu.financialcrisis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计时间线查询请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTimelineRequest {

    private Boolean includeRawPayload;
}
