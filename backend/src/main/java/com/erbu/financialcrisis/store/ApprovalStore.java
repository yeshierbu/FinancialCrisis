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
 * 审批数据持久化门面。
 * 统一封装各 MyBatis Mapper，避免业务层直接组合多张表的读写逻辑。
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

    /** 新增或更新贷款申请。 */
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

    /**
     * 更新申请状态，并同时写入一条状态流转日志。
     * 调用方应在事务中执行，保证主表状态和审计记录一致。
     */
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

    /** 保存申请材料元数据。 */
    public void addDocument(UploadedDocument document) {
        uploadedDocumentMapper.insert(document);
    }

    public void updateDocument(UploadedDocument document) {
        uploadedDocumentMapper.updateByDocumentId(document);
    }

    public List<UploadedDocument> listDocuments(Long applicationId) {
        return uploadedDocumentMapper.selectByApplicationId(applicationId);
    }

    /** 按申请维度新增或更新反欺诈结果。 */
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

    /** 按申请维度新增或更新偿债能力结果。 */
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

    /** 按申请维度新增或更新最终审批决定。 */
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

    /** 按申请维度新增或更新人工复核工单。 */
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

    /** 按申请维度新增或更新审批报告。 */
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

    /** 保存一次 Agent 执行记录，供审计时间线回放。 */
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
