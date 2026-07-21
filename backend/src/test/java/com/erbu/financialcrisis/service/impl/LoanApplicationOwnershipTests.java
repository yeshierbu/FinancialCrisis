package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.security.CurrentAccount;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanApplicationOwnershipTests {
    @Test void normalUserOnlySeesOwnedApplications() {
        ApprovalStore store = mock(ApprovalStore.class);
        CurrentAccount account = mock(CurrentAccount.class);
        when(account.username()).thenReturn("alice");
        when(account.privileged()).thenReturn(false);
        LoanApplication mine = new LoanApplication(); mine.setApplicationId(1L); mine.setOwnerUsername("alice");
        LoanApplication other = new LoanApplication(); other.setApplicationId(2L); other.setOwnerUsername("bob");
        when(store.listApplicationsOwnedBy("alice")).thenReturn(List.of(mine));

        var result = new LoanApplicationServiceImpl(store, account).listApplications();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getApplicationId());
    }
}
