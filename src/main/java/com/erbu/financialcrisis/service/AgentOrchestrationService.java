package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.domain.entity.LoanApplication;

/**
 * Agent 编排服务。
 * 这里是整个审批流程的大脑，负责串起信息采集、风控、偿债能力和决策 Agent。
 */
public interface AgentOrchestrationService {

    void startApprovalFlow(LoanApplication application);
}
