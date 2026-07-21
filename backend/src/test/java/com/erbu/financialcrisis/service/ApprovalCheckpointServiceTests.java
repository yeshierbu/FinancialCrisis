package com.erbu.financialcrisis.service;

import com.erbu.financialcrisis.agent.artifact.RiskReport;
import com.erbu.financialcrisis.domain.entity.ApprovalStepCheckpoint;
import com.erbu.financialcrisis.mapper.ApprovalStepCheckpointMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalCheckpointServiceTests {
    @Test void restoredCheckpointDoesNotExecuteExternalWorkAgain() throws Exception {
        ApprovalStepCheckpointMapper mapper = mock(ApprovalStepCheckpointMapper.class);
        ApprovalStepCheckpoint checkpoint = new ApprovalStepCheckpoint();
        checkpoint.setStateJson(new ObjectMapper().writeValueAsString(new RiskReport()));
        when(mapper.select(1L, "RISK_ANALYSIS")).thenReturn(checkpoint);
        ApprovalCheckpointService service = new ApprovalCheckpointService(mapper, new ObjectMapper());
        AtomicBoolean executed = new AtomicBoolean();

        assertNotNull(service.restoreOrExecute(1L, "RISK_ANALYSIS", RiskReport.class, () -> {
            executed.set(true); return new RiskReport();
        }));
        assertFalse(executed.get());
        verify(mapper, never()).insert(any());
    }
}
