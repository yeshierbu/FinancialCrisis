package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 状态流转日志表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateTransitionLog {

    private Long logId;
    private Long applicationId;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private String triggerEvent;
    private OperatorType operatorType;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
}
