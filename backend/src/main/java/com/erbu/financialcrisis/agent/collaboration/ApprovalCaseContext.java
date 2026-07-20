package com.erbu.financialcrisis.agent.collaboration;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次审批运行期间的共享案件上下文。
 *
 * <p>它是轻量级的黑板（blackboard）：各专业 Agent 写入结构化发现，Supervisor 再把这些发现
 * 交给审查 Agent 和最终规则决策。当前上下文生命周期只覆盖一次同步审批流程，持久化审计仍由
 * AgentTaskLog 和状态日志负责。</p>
 */
public class ApprovalCaseContext {

    private final Long applicationId;
    private final List<AgentFinding> findings = new ArrayList<>();

    public ApprovalCaseContext(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void addFinding(AgentFinding finding) {
        if (finding != null) {
            findings.add(finding);
                              }
    }

    public List<AgentFinding> getFindings() {
        return List.copyOf(findings);
    }
}
