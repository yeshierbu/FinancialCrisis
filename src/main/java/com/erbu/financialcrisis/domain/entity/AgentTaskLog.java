package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.LogStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 任务日志表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskLog {

    private Long logId;
    private Long applicationId;
    private String agentName;
    private String taskName;
    private String inputSummary;
    private String outputSummary;
    private LogStatus status;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
