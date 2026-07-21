package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.ApprovalOutbox;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.mapper.ApprovalOutboxMapper;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.store.ApprovalStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.erbu.financialcrisis.domain.enums.ApprovalStep;

class ApprovalTaskServiceImplTests {

    @Test
    void enabledMessagingMustWriteOutboxWithoutRunningApprovalOnRequestThread() {
        ApprovalStore store = mock(ApprovalStore.class);
        ApprovalOutboxMapper mapper = mock(ApprovalOutboxMapper.class);
        AgentOrchestrationService orchestration = mock(AgentOrchestrationService.class);
        LoanApplication application = new LoanApplication();
        application.setApplicationId(7L);
        application.setApplicationNo("APP-7");
        when(store.getApplicationOrThrow(7L)).thenReturn(application);
        ApprovalTaskServiceImpl service = new ApprovalTaskServiceImpl(
                store, mapper, orchestration, new ObjectMapper().findAndRegisterModules(), true);

        String eventId = service.submit(7L);

        assertThat(eventId).isNotBlank();
        verify(mapper).insert(any(ApprovalOutbox.class));
        verify(orchestration, never()).executeStep(any(), any());
    }

    @Test
    void disabledMessagingMayUseSynchronousFallbackForTests() {
        AgentOrchestrationService orchestration = mock(AgentOrchestrationService.class);
        ApprovalTaskServiceImpl service = new ApprovalTaskServiceImpl(
                mock(ApprovalStore.class), mock(ApprovalOutboxMapper.class), orchestration,
                new ObjectMapper(), false);

        assertThat(service.submit(9L)).isEqualTo("SYNC-9");
        verify(orchestration).executeStep(9L, ApprovalStep.DOCUMENT_INTAKE);
    }
}
