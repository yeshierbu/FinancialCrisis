package com.erbu.financialcrisis.dto.response;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交补充资料响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplementResponse {

    private Long applicationId;
    private ApplicationStatus status;
    private String currentStep;
}
