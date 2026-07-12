package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.AgentTaskLog;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.PolicyHitRecord;
import com.erbu.financialcrisis.domain.entity.StateTransitionLog;
import com.erbu.financialcrisis.domain.entity.ToolCallLog;
import com.erbu.financialcrisis.dto.request.AuditTimelineRequest;
import com.erbu.financialcrisis.dto.response.AuditTimelineItemResponse;
import com.erbu.financialcrisis.dto.response.AuditTimelineResponse;
import com.erbu.financialcrisis.service.AuditTimelineService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 审计时间线业务服务实现。
 *
 * <p>审计接口不是普通展示接口，它用于回答“审批过程发生过什么”。因此这里把状态日志、
 * Agent 执行日志、工具调用日志和政策命中记录统一按时间排序，形成一条可回放时间线。</p>
 */
@Service
public class AuditTimelineServiceImpl implements AuditTimelineService {

    private final ApprovalStore store;

    public AuditTimelineServiceImpl(ApprovalStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditTimelineResponse getTimeline(Long applicationId, AuditTimelineRequest request) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        boolean includeRawPayload = request != null && Boolean.TRUE.equals(request.getIncludeRawPayload());
        List<AuditTimelineItemResponse> timeline = new ArrayList<>();

        for (StateTransitionLog log : store.listStateLogs(applicationId)) {
            timeline.add(new AuditTimelineItemResponse(
                    "STATUS",
                    String.valueOf(log.getFromStatus()) + " -> " + log.getToStatus(),
                    log.getOperatorName(),
                    log.getRemark(),
                    log.getCreatedAt()
            ));
        }

        for (AgentTaskLog log : store.listAgentLogs(applicationId)) {
            timeline.add(new AuditTimelineItemResponse(
                    "AGENT",
                    log.getAgentName() + "/" + log.getTaskName(),
                    "Agent",
                    includeRawPayload ? log.getInputSummary() + " | " + log.getOutputSummary() : log.getOutputSummary(),
                    log.getFinishedAt()
            ));
        }

        for (ToolCallLog log : store.listToolLogs(applicationId)) {
            timeline.add(new AuditTimelineItemResponse(
                    "TOOL",
                    log.getToolName(),
                    log.getAgentName(),
                    includeRawPayload ? log.getRequestPayload() + " | " + log.getResponsePayload() : log.getCallStatus().name(),
                    log.getCalledAt()
            ));
        }

        for (PolicyHitRecord record : store.listPolicyHits(applicationId)) {
            timeline.add(new AuditTimelineItemResponse(
                    "POLICY",
                    record.getPolicyTitle() + "#" + record.getClauseId(),
                    record.getHitSource(),
                    record.getClauseText(),
                    record.getCreatedAt()
            ));
        }

        timeline.sort(Comparator.comparing(item -> safeTime(item.getOccurredAt())));
        return new AuditTimelineResponse(
                application.getApplicationId(),
                application.getApplicationNo(),
                application.getStatus(),
                timeline
        );
    }

    private LocalDateTime safeTime(LocalDateTime time) {
        return time == null ? LocalDateTime.MIN : time;
    }
}
