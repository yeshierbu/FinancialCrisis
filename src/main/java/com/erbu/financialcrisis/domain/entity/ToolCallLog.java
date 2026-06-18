package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.LogStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工具调用日志表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallLog {

    private Long logId;
    private Long applicationId;
    private String agentName;
    private String toolName;
    private String requestPayload;
    private String responsePayload;
    private LogStatus callStatus;
    private Integer retryCount;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime calledAt;
    private LocalDateTime createdAt;
}
