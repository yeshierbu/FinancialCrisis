package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.ApprovalMessageConsumeLog;
import com.erbu.financialcrisis.mapper.ApprovalMessageConsumeLogMapper;
import com.erbu.financialcrisis.mapper.ApprovalOutboxMapper;
import com.erbu.financialcrisis.messaging.ApprovalStartMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import com.erbu.financialcrisis.domain.enums.ApprovalStep;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApprovalConsumeServiceImplTests {
    @Test void processingEventCanBeReclaimedOnlyWhenMapperLeaseClaimSucceeds() {
        ApprovalMessageConsumeLogMapper consume = mock(ApprovalMessageConsumeLogMapper.class);
        ApprovalOutboxMapper outbox = mock(ApprovalOutboxMapper.class);
        ApprovalMessageConsumeLog existing = new ApprovalMessageConsumeLog();
        existing.setConsumeStatus("PROCESSING");
        when(consume.selectByEventId("evt")).thenReturn(existing);
        when(consume.retryClaim(eq("evt"), eq("consumer-2"), eq(1), anyString(), any())).thenReturn(1);
        ApprovalConsumeServiceImpl service = new ApprovalConsumeServiceImpl(consume, outbox, 120);

        String token = service.tryStart(new ApprovalStartMessage("evt", 1L, "APP", ApprovalStep.RISK_ANALYSIS, 1, LocalDateTime.now()), "consumer-2");
        assertNotNull(token);
        service.complete("evt", token);
        verify(consume).markCompleted("evt", token);
    }

    @Test void activeLeaseClaimFailureIsNotProcessed() {
        ApprovalMessageConsumeLogMapper consume = mock(ApprovalMessageConsumeLogMapper.class);
        ApprovalMessageConsumeLog existing = new ApprovalMessageConsumeLog();
        existing.setConsumeStatus("PROCESSING");
        when(consume.selectByEventId("evt")).thenReturn(existing);
        when(consume.retryClaim(anyString(), anyString(), anyInt(), anyString(), any())).thenReturn(0);
        ApprovalConsumeServiceImpl service = new ApprovalConsumeServiceImpl(consume, mock(ApprovalOutboxMapper.class), 120);
        assertNull(service.tryStart(new ApprovalStartMessage("evt", 1L, "APP", ApprovalStep.RISK_ANALYSIS, 0, LocalDateTime.now()), "consumer"));
    }
}
