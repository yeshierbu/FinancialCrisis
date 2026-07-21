package com.erbu.financialcrisis.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalMessageConsumeLog {
    private Long id;
    private String eventId;
    private Long applicationId;
    private String consumeStatus;
    private String consumerName;
    private Integer retryCount;
    private String claimToken;
    private LocalDateTime leaseUntil;
    private Integer attemptNo;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
