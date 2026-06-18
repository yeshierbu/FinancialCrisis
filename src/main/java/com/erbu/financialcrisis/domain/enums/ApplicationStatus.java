package com.erbu.financialcrisis.domain.enums;

/**
 * 审批主流程状态。
 * 这类枚举后续既可以用于接口返回，也可以用于状态机流转判断。
 */
public enum ApplicationStatus {
    SUBMITTED,
    MATERIAL_PENDING,
    OCR_PARSING,
    EXTERNAL_VERIFYING,
    RISK_ANALYZING,
    DECISIONING,
    MANUAL_REVIEW,
    APPROVED,
    REJECTED,
    ARCHIVED
}
