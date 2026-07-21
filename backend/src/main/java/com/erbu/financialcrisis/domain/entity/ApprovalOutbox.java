package com.erbu.financialcrisis.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalOutbox {
    private Long id;
    private String eventId;
    private Long applicationId;
    private String eventType;
    private String routingKey;
    private String payloadJson;
    private String publishStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
