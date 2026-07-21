package com.erbu.financialcrisis.domain;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Central allow-list for application state transitions. */
@Component
public class ApplicationStateMachine {
    private final Map<ApplicationStatus, EnumSet<ApplicationStatus>> allowed = new EnumMap<>(ApplicationStatus.class);

    public ApplicationStateMachine() {
        allow(ApplicationStatus.SUBMITTED, ApplicationStatus.DOCUMENT_PENDING, ApplicationStatus.MATERIAL_PENDING,
                ApplicationStatus.OCR_PARSING, ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.ARCHIVED);
        allow(ApplicationStatus.DOCUMENT_PENDING, ApplicationStatus.SUBMITTED, ApplicationStatus.MATERIAL_PENDING,
                ApplicationStatus.OCR_PARSING, ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.ARCHIVED);
        allow(ApplicationStatus.MATERIAL_PENDING, ApplicationStatus.SUBMITTED, ApplicationStatus.DOCUMENT_PENDING,
                ApplicationStatus.OCR_PARSING, ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.ARCHIVED);
        allow(ApplicationStatus.OCR_PARSING, ApplicationStatus.DOCUMENT_PENDING, ApplicationStatus.RISK_ANALYZING,
                ApplicationStatus.MANUAL_REVIEW);
        allow(ApplicationStatus.EXTERNAL_VERIFYING, ApplicationStatus.RISK_ANALYZING, ApplicationStatus.MANUAL_REVIEW);
        allow(ApplicationStatus.RISK_ANALYZING, ApplicationStatus.DECISION_PENDING, ApplicationStatus.MANUAL_REVIEW);
        allow(ApplicationStatus.DECISION_PENDING, ApplicationStatus.DECISIONING, ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED, ApplicationStatus.MANUAL_REVIEW);
        allow(ApplicationStatus.DECISIONING, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED,
                ApplicationStatus.MANUAL_REVIEW);
        allow(ApplicationStatus.MANUAL_REVIEW, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED,
                ApplicationStatus.ARCHIVED);
    }

    private void allow(ApplicationStatus from, ApplicationStatus... to) {
        allowed.put(from, EnumSet.copyOf(java.util.List.of(to)));
    }

    public void assertAllowed(ApplicationStatus from, ApplicationStatus to) {
        if (from == to) return; // idempotent status refreshes are audited by design
        if (from == null || to == null || !allowed.getOrDefault(from, EnumSet.noneOf(ApplicationStatus.class)).contains(to)) {
            throw new BusinessException(4003, "非法状态迁移：" + from + " -> " + to);
        }
    }
}
