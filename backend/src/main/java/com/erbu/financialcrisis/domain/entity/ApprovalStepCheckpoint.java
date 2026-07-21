package com.erbu.financialcrisis.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApprovalStepCheckpoint {
    private Long id;
    private Long applicationId;
    private String stepName;
    private String stateJson;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
