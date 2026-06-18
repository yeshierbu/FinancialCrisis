package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;

/**
 * 贷款申请响应。
 */
public class LoanApplicationResponse {

    private Long applicationId;
    private String applicationNo;
    private ApplicationStatus status;
    private String currentStep;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationNo() {
        return applicationNo;
    }

    public void setApplicationNo(String applicationNo) {
        this.applicationNo = applicationNo;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
}
