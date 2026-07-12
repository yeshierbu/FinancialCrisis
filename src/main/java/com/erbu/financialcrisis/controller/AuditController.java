package com.erbu.financialcrisis.controller;

import com.erbu.financialcrisis.common.Result;
import com.erbu.financialcrisis.dto.request.AuditTimelineRequest;
import com.erbu.financialcrisis.dto.response.AuditTimelineResponse;
import com.erbu.financialcrisis.service.AuditTimelineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端审计入口。
 */
@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

    private final AuditTimelineService auditTimelineService;

    public AuditController(AuditTimelineService auditTimelineService) {
        this.auditTimelineService = auditTimelineService;
    }

    /** 聚合状态、Agent、工具和政策命中记录，生成审批审计时间线。 */
    @GetMapping("/{applicationId}/timeline")
    public Result<AuditTimelineResponse> getTimeline(@PathVariable Long applicationId,
                                                     @ModelAttribute AuditTimelineRequest request) {
        return Result.success(auditTimelineService.getTimeline(applicationId, request));
    }
}
