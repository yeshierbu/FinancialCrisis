package com.erbu.financialcrisis.service;

/**
 * Agent 编排服务。
 * 这里是整个审批流程的大脑，负责串起信息采集、风控、偿债能力和决策 Agent。
 */
public interface AgentOrchestrationService {

    /** 按既定顺序执行各专业 Agent，并持久化过程与结果。 */
    void startApprovalFlow(Long applicationId);

    /** 消息重试耗尽后安全降级为人工审核。 */
    void moveToManualReview(Long applicationId, String reason);
}
