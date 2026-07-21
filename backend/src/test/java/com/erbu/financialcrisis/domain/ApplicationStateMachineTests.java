package com.erbu.financialcrisis.domain;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationStateMachineTests {
    private final ApplicationStateMachine machine = new ApplicationStateMachine();

    @Test void allowsExpectedApprovalPath() {
        assertDoesNotThrow(() -> machine.assertAllowed(ApplicationStatus.SUBMITTED, ApplicationStatus.OCR_PARSING));
        assertDoesNotThrow(() -> machine.assertAllowed(ApplicationStatus.RISK_ANALYZING, ApplicationStatus.DECISION_PENDING));
        assertDoesNotThrow(() -> machine.assertAllowed(ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.APPROVED));
    }

    @Test void terminalStateCannotRestart() {
        assertThrows(BusinessException.class,
                () -> machine.assertAllowed(ApplicationStatus.APPROVED, ApplicationStatus.OCR_PARSING));
    }
}
