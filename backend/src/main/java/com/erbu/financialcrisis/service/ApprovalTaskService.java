package com.erbu.financialcrisis.service;

/** 将一笔申请提交到异步审批通道。 */
public interface ApprovalTaskService {
    String submit(Long applicationId);
    String submitStep(Long applicationId, com.erbu.financialcrisis.domain.enums.ApprovalStep step);
    String submitStep(Long applicationId, com.erbu.financialcrisis.domain.enums.ApprovalStep step, String parentEventId);
}
