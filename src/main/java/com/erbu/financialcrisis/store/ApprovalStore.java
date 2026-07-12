package com.erbu.financialcrisis.store;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.*;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.mapper.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis-backed persistence facade used by the approval services.
 */
@Component
public class ApprovalStore {

    private final LoanApplicationMapper loanApplicationMapper;
    private final UploadedDocumentMapper uploadedDocumentMapper;
    private final StateTransitionLogMapper stateTransitionLogMapper;
    private final AgentTaskLogMapper agentTaskLogMapper;
    private final FraudRiskResultMapper fraudRiskResultMapper;
    private final RepaymentCapacityResultMapper repaymentCapacityResultMapper;
    private final ApprovalDecisionMapper approvalDecisionMapper;
    private final ManualReviewTicketMapper manualReviewTicketMapper;
    private final ApprovalReportMapper approvalReportMapper;
    private final ToolCallLogMapper toolCallLogMapper;
    private final PolicyHitRecordMapper policyHitRecordMapper;

    public ApprovalStore(LoanApplicationMapper loanApplicationMapper,
                         UploadedDocumentMapper uploadedDocumentMapper,
                         StateTransitionLogMapper stateTransitionLogMapper,
                         AgentTaskLogMapper agentTaskLogMapper,
                         FraudRiskResultMapper fraudRiskResultMapper,
                         RepaymentCapacityResultMapper repaymentCapacityResultMapper,
                         ApprovalDecisionMapper approvalDecisionMapper,
                         ManualReviewTicketMapper manualReviewTicketMapper,
                         ApprovalReportMapper approvalReportMapper,
                         ToolCallLogMapper toolCallLogMapper,
                         PolicyHitRecordMapper policyHitRecordMapper) {
        this.loanApplicationMapper = loanApplicationMapper;
        this.uploadedDocumentMapper = uploadedDocumentMapper;
        this.stateTransitionLogMapper = stateTransitionLogMapper;
        this.agentTaskLogMapper = agentTaskLogMapper;
        this.fraudRiskResultMapper = fraudRiskResultMapper;
        this.repaymentCapacityResultMapper = repaymentCapacityResultMapper;
        this.approvalDecisionMapper = approvalDecisionMapper;
        this.manualReviewTicketMapper = manualReviewTicketMapper;
        this.approvalReportMapper = approvalReportMapper;
        this.toolCallLogMapper = toolCallLogMapper;
        this.policyHitRecordMapper = policyHitRecordMapper;
    }

    public void saveApplication(LoanApplication application) {
        if (application.getApplicationId() == null) {
            loanApplicationMapper.insert(application);
        } else {
            loanApplicationMapper.updateByApplicationId(application);
        }
    }

    public Optional<LoanApplication> findApplication(Long applicationId) {
        return Optional.ofNullable(loanApplicationMapper.selectByApplicationId(applicationId));
    }

    public LoanApplication getApplicationOrThrow(Long applicationId) {
        return findApplication(applicationId)
                .orElseThrow(() -> new BusinessException(4002, "申请单不存在"));
    }

    public List<LoanApplication> listApplications() {
        return loanApplicationMapper.selectAll();
    }

    public void changeStatus(LoanApplication application,
                             ApplicationStatus toStatus,
                             String currentStep,
                             String triggerEvent,
                             OperatorType operatorType,
                             String operatorName,
                             String remark) {
        ApplicationStatus fromStatus = application.getStatus();
        LocalDateTime now = LocalDateTime.now();
        application.setStatus(toStatus);
        application.setCurrentStep(currentStep);
        application.setUpdatedAt(now);
        loanApplicationMapper.updateByApplicationId(application);

        stateTransitionLogMapper.insert(new StateTransitionLog(
                null, application.getApplicationId(), fromStatus, toStatus, triggerEvent,
                operatorType, operatorName, remark, now
        ));
    }

    public void addDocument(UploadedDocument document) {
        uploadedDocumentMapper.insert(document);
    }

    public void updateDocument(UploadedDocument document) {
        uploadedDocumentMapper.updateByDocumentId(document);
    }

    public List<UploadedDocument> listDocuments(Long applicationId) {
        return uploadedDocumentMapper.selectByApplicationId(applicationId);
    }

    public void saveFraudResult(FraudRiskResult result) {
        if (fraudRiskResultMapper.selectByApplicationId(result.getApplicationId()) == null) {
            fraudRiskResultMapper.insert(result);
        } else {
            fraudRiskResultMapper.updateByApplicationId(result);
        }
    }

    public Optional<FraudRiskResult> findFraudResult(Long applicationId) {
        return Optional.ofNullable(fraudRiskResultMapper.selectByApplicationId(applicationId));
    }

    public void saveRepaymentResult(RepaymentCapacityResult result) {
        if (repaymentCapacityResultMapper.selectByApplicationId(result.getApplicationId()) == null) {
            repaymentCapacityResultMapper.insert(result);
        } else {
            repaymentCapacityResultMapper.updateByApplicationId(result);
        }
    }

    public Optional<RepaymentCapacityResult> findRepaymentResult(Long applicationId) {
        return Optional.ofNullable(repaymentCapacityResultMapper.selectByApplicationId(applicationId));
    }

    public void saveApprovalDecision(ApprovalDecision decision) {
        ApprovalDecision existing = approvalDecisionMapper.selectByApplicationId(decision.getApplicationId());
        if (existing == null) {
            approvalDecisionMapper.insert(decision);
        } else {
            decision.setId(existing.getId());
            approvalDecisionMapper.updateByApplicationId(decision);
        }
    }

    public Optional<ApprovalDecision> findApprovalDecision(Long applicationId) {
        return Optional.ofNullable(approvalDecisionMapper.selectByApplicationId(applicationId));
    }

    public void saveReviewTicket(ManualReviewTicket ticket) {
        ManualReviewTicket existing = manualReviewTicketMapper.selectByApplicationId(ticket.getApplicationId());
        if (existing == null) {
            manualReviewTicketMapper.insert(ticket);
        } else {
            ticket.setTicketId(existing.getTicketId());
            manualReviewTicketMapper.updateByApplicationId(ticket);
        }
    }

    public Optional<ManualReviewTicket> findReviewTicket(Long applicationId) {
        return Optional.ofNullable(manualReviewTicketMapper.selectByApplicationId(applicationId));
    }

    public List<ManualReviewTicket> listReviewTickets() {
        return manualReviewTicketMapper.selectPendingList();
    }

    public void saveApprovalReport(ApprovalReport report) {
        ApprovalReport existing = approvalReportMapper.selectByApplicationId(report.getApplicationId());
        if (existing == null) {
            approvalReportMapper.insert(report);
        } else {
            report.setReportId(existing.getReportId());
            approvalReportMapper.updateByApplicationId(report);
        }
    }

    public Optional<ApprovalReport> findApprovalReport(Long applicationId) {
        return Optional.ofNullable(approvalReportMapper.selectByApplicationId(applicationId));
    }

    public void addPolicyHit(PolicyHitRecord record) {
        policyHitRecordMapper.insert(record);
    }

    public List<PolicyHitRecord> listPolicyHits(Long applicationId) {
        return policyHitRecordMapper.selectByApplicationId(applicationId);
    }

    public void addToolLog(ToolCallLog log) {
        toolCallLogMapper.insert(log);
    }

    public List<ToolCallLog> listToolLogs(Long applicationId) {
        return toolCallLogMapper.selectByApplicationId(applicationId);
    }

    public void addAgentLog(AgentTaskLog log) {
        agentTaskLogMapper.insert(log);
    }

    public List<AgentTaskLog> listAgentLogs(Long applicationId) {
        return agentTaskLogMapper.selectByApplicationId(applicationId);
    }

    public List<StateTransitionLog> listStateLogs(Long applicationId) {
        return stateTransitionLogMapper.selectByApplicationId(applicationId);
    }
}
