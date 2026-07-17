package com.erbu.financialcrisis.domain.entity;

import com.erbu.financialcrisis.domain.enums.LogStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 调用轨迹表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmTraceLog {

    private Long logId;
    private Long applicationId;
    private String agentName;
    private String modelName;
    private String promptSummary;
    private String responseSummary;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long durationMs;
    private LogStatus traceStatus;
    private LocalDateTime createdAt;
}
