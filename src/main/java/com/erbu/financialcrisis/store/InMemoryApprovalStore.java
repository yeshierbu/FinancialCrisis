package com.erbu.financialcrisis.store;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.AgentTaskLog;
import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import com.erbu.financialcrisis.domain.entity.ApprovalReport;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.ManualReviewTicket;
import com.erbu.financialcrisis.domain.entity.PolicyHitRecord;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.entity.StateTransitionLog;
import com.erbu.financialcrisis.domain.entity.ToolCallLog;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 第一阶段使用的内存数据中心。
 *
 * <p>这个类刻意模拟数据库表的边界，而不是把所有状态散落在各个 Service 中。这样做有三个好处：
 * 1. 主流程马上可运行；2. 所有状态流转和审计日志都能被统一查询；3. 后续切换到 MySQL/MyBatis 时，
 * 每个 Map 基本都能对应到 SQL 文档里的一张表。</p>
 *
 * <p>注意：内存存储只适合演示和单机开发，进程重启后数据会丢失。真实生产版本应把这里的方法逐步替换为
 * Mapper/Repository 调用，并在写申请、写决策、写状态日志时加事务。</p>
 */
@Component
public class InMemoryApprovalStore {

    private final AtomicLong applicationIdSequence = new AtomicLong(100000);
    private final AtomicLong documentIdSequence = new AtomicLong(200000);
    private final AtomicLong stateLogIdSequence = new AtomicLong(300000);
    private final AtomicLong agentLogIdSequence = new AtomicLong(400000);
    private final AtomicLong fraudResultIdSequence = new AtomicLong(500000);
    private final AtomicLong repaymentResultIdSequence = new AtomicLong(600000);
    private final AtomicLong decisionIdSequence = new AtomicLong(700000);
    private final AtomicLong ticketIdSequence = new AtomicLong(800000);
    private final AtomicLong reportIdSequence = new AtomicLong(900000);
    private final AtomicLong toolLogIdSequence = new AtomicLong(1000000);
    private final AtomicLong policyHitIdSequence = new AtomicLong(1100000);

    private final Map<Long, LoanApplication> applications = new ConcurrentHashMap<>();
    private final Map<Long, List<UploadedDocument>> documents = new ConcurrentHashMap<>();
    private final Map<Long, List<StateTransitionLog>> stateLogs = new ConcurrentHashMap<>();
    private final Map<Long, List<AgentTaskLog>> agentLogs = new ConcurrentHashMap<>();
    private final Map<Long, FraudRiskResult> fraudResults = new ConcurrentHashMap<>();
    private final Map<Long, RepaymentCapacityResult> repaymentResults = new ConcurrentHashMap<>();
    private final Map<Long, ApprovalDecision> approvalDecisions = new ConcurrentHashMap<>();
    private final Map<Long, ManualReviewTicket> reviewTickets = new ConcurrentHashMap<>();
    private final Map<Long, ApprovalReport> approvalReports = new ConcurrentHashMap<>();
    private final Map<Long, List<ToolCallLog>> toolLogs = new ConcurrentHashMap<>();
    private final Map<Long, List<PolicyHitRecord>> policyHits = new ConcurrentHashMap<>();

    public Long nextApplicationId() {
        return applicationIdSequence.incrementAndGet();
    }

    public Long nextDocumentId() {
        return documentIdSequence.incrementAndGet();
    }

    public Long nextTicketId() {
        return ticketIdSequence.incrementAndGet();
    }

    public Long nextReportId() {
        return reportIdSequence.incrementAndGet();
    }

    public Long nextFraudResultId() {
        return fraudResultIdSequence.incrementAndGet();
    }

    public Long nextRepaymentResultId() {
        return repaymentResultIdSequence.incrementAndGet();
    }

    public Long nextDecisionId() {
        return decisionIdSequence.incrementAndGet();
    }

    public Long nextPolicyHitId() {
        return policyHitIdSequence.incrementAndGet();
    }

    public void saveApplication(LoanApplication application) {
        applications.put(application.getApplicationId(), application);
    }

    public Optional<LoanApplication> findApplication(Long applicationId) {
        return Optional.ofNullable(applications.get(applicationId));
    }

    public LoanApplication getApplicationOrThrow(Long applicationId) {
        return findApplication(applicationId)
                .orElseThrow(() -> new BusinessException(4002, "申请单不存在"));
    }

    public List<LoanApplication> listApplications() {
        return applications.values().stream()
                .sorted(Comparator.comparing(LoanApplication::getCreatedAt))
                .toList();
    }

    /**
     * 统一推进申请状态并写入状态流转日志。
     *
     * <p>状态字段本身只能告诉我们“现在是什么”，而日志能告诉我们“为什么走到这里”。审批系统后续排查、
     * 面试讲解和审计回放都依赖这条记录，所以所有 Service 和 Orchestrator 都必须通过这个方法改状态。</p>
     */
    public synchronized void changeStatus(LoanApplication application,
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
        applications.put(application.getApplicationId(), application);

        StateTransitionLog log = new StateTransitionLog(
                stateLogIdSequence.incrementAndGet(),
                application.getApplicationId(),
                fromStatus,
                toStatus,
                triggerEvent,
                operatorType,
                operatorName,
                remark,
                now
        );
        stateLogs.computeIfAbsent(application.getApplicationId(), key -> new CopyOnWriteArrayList<>()).add(log);
    }

    public void addDocument(UploadedDocument document) {
        documents.computeIfAbsent(document.getApplicationId(), key -> new CopyOnWriteArrayList<>()).add(document);
    }

    public List<UploadedDocument> listDocuments(Long applicationId) {
        return new ArrayList<>(documents.getOrDefault(applicationId, List.of()));
    }

    public void saveFraudResult(FraudRiskResult result) {
        fraudResults.put(result.getApplicationId(), result);
    }

    public Optional<FraudRiskResult> findFraudResult(Long applicationId) {
        return Optional.ofNullable(fraudResults.get(applicationId));
    }

    public void saveRepaymentResult(RepaymentCapacityResult result) {
        repaymentResults.put(result.getApplicationId(), result);
    }

    public Optional<RepaymentCapacityResult> findRepaymentResult(Long applicationId) {
        return Optional.ofNullable(repaymentResults.get(applicationId));
    }

    public void saveApprovalDecision(ApprovalDecision decision) {
        approvalDecisions.put(decision.getApplicationId(), decision);
    }

    public Optional<ApprovalDecision> findApprovalDecision(Long applicationId) {
        return Optional.ofNullable(approvalDecisions.get(applicationId));
    }

    public void saveReviewTicket(ManualReviewTicket ticket) {
        reviewTickets.put(ticket.getApplicationId(), ticket);
    }

    public Optional<ManualReviewTicket> findReviewTicket(Long applicationId) {
        return Optional.ofNullable(reviewTickets.get(applicationId));
    }

    public List<ManualReviewTicket> listReviewTickets() {
        return reviewTickets.values().stream()
                .sorted(Comparator.comparing(ManualReviewTicket::getCreatedAt))
                .toList();
    }

    public void saveApprovalReport(ApprovalReport report) {
        approvalReports.put(report.getApplicationId(), report);
    }

    public Optional<ApprovalReport> findApprovalReport(Long applicationId) {
        return Optional.ofNullable(approvalReports.get(applicationId));
    }

    public void addPolicyHit(PolicyHitRecord record) {
        policyHits.computeIfAbsent(record.getApplicationId(), key -> new CopyOnWriteArrayList<>()).add(record);
    }

    public List<PolicyHitRecord> listPolicyHits(Long applicationId) {
        return new ArrayList<>(policyHits.getOrDefault(applicationId, List.of()));
    }

    public void addToolLog(ToolCallLog log) {
        toolLogs.computeIfAbsent(log.getApplicationId(), key -> new CopyOnWriteArrayList<>()).add(log);
    }

    public Long nextToolLogId() {
        return toolLogIdSequence.incrementAndGet();
    }

    public List<ToolCallLog> listToolLogs(Long applicationId) {
        return new ArrayList<>(toolLogs.getOrDefault(applicationId, List.of()));
    }

    public void addAgentLog(AgentTaskLog log) {
        agentLogs.computeIfAbsent(log.getApplicationId(), key -> new CopyOnWriteArrayList<>()).add(log);
    }

    public Long nextAgentLogId() {
        return agentLogIdSequence.incrementAndGet();
    }

    public List<AgentTaskLog> listAgentLogs(Long applicationId) {
        return new ArrayList<>(agentLogs.getOrDefault(applicationId, List.of()));
    }

    public List<StateTransitionLog> listStateLogs(Long applicationId) {
        return new ArrayList<>(stateLogs.getOrDefault(applicationId, List.of()));
    }
}
