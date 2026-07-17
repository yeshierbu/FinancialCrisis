package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.dto.request.AuditTimelineRequest;
import com.erbu.financialcrisis.dto.response.AuditTimelineResponse;

/**
 * 审计时间线服务接口。
 */
public interface AuditTimelineService {

    /** 生成指定申请的完整审计时间线。 */
    AuditTimelineResponse getTimeline(Long applicationId, AuditTimelineRequest request);
}
