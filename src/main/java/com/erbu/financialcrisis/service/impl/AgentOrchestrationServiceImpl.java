package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.agent.ComplianceDecisionAgent;
import com.erbu.financialcrisis.agent.DocumentIntakeAgent;
import com.erbu.financialcrisis.agent.FraudRiskAgent;
import com.erbu.financialcrisis.agent.RepaymentCapacityAgent;
import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.AgentTaskLog;
import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.ManualReviewTicket;
import com.erbu.financialcrisis.domain.entity.PolicyHitRecord;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.entity.UploadedDocument;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.LogStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.store.InMemoryApprovalStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 审批编排业务服务实现。
 *
 * <p>这一层是多 Agent 项目的“流程大脑”：它不直接写风控规则，也不直接解析材料，而是负责
 * 决定各个 Agent 的执行顺序、统一推进状态、统一记录 Agent 日志，并在异常时把流程转入人工兜底。</p>
 */
@Service
public class AgentOrchestrationServiceImpl implements AgentOrchestrationService {

    private final InMemoryApprovalStore store;
    private final DocumentIntakeAgent documentIntakeAgent;
    private final FraudRiskAgent fraudRiskAgent;
    private final RepaymentCapacityAgent repaymentCapacityAgent;
    private final ComplianceDecisionAgent complianceDecisionAgent;

    public AgentOrchestrationServiceImpl(InMemoryApprovalStore store,
                                         DocumentIntakeAgent documentIntakeAgent,
                                         FraudRiskAgent fraudRiskAgent,
                                         RepaymentCapacityAgent repaymentCapacityAgent,
                                         ComplianceDecisionAgent complianceDecisionAgent) {
        this.store = store;
        this.documentIntakeAgent = documentIntakeAgent;
        this.fraudRiskAgent = fraudRiskAgent;
        this.repaymentCapacityAgent = repaymentCapacityAgent;
        this.complianceDecisionAgent = complianceDecisionAgent;
    }

    @Override
    public void startApprovalFlow(Long applicationId) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);

        try {
            store.changeStatus(
                    application,
                    ApplicationStatus.OCR_PARSING,
                    "信息采集 Agent 解析申请材料",
                    "START_APPROVAL_FLOW",
                    OperatorType.SYSTEM,
                    "orchestrator",
                    "启动同步审批编排流程"
            );

            List<UploadedDocument> documents = store.listDocuments(applicationId);
            DocumentIntakeResult documentResult = runAgent(
                    applicationId,
                    "DocumentIntakeAgent",
                    "材料完整性校验与模拟 OCR",
                    "documents=" + documents.size(),
                    () -> documentIntakeAgent.collectAndParse(application, documents)
            );

            if (Boolean.TRUE.equals(documentResult.getNeedSupplement())) {
                store.changeStatus(
                        application,
                        ApplicationStatus.DOCUMENT_PENDING,
                        "等待用户补充材料：" + documentResult.getMissingDocuments(),
                        "DOCUMENT_SUPPLEMENT_REQUIRED",
                        OperatorType.AGENT,
                        "DocumentIntakeAgent",
                        documentResult.getSummary()
                );
                return;
            }

            store.changeStatus(
                    application,
                    ApplicationStatus.RISK_ANALYZING,
                    "反欺诈风控与偿债能力分析中",
                    "DOCUMENT_INTAKE_FINISHED",
                    OperatorType.AGENT,
                    "DocumentIntakeAgent",
                    documentResult.getSummary()
            );

            FraudRiskResult fraudRiskResult = runAgent(
                    applicationId,
                    "FraudRiskAgent",
                    "反欺诈规则评估",
                    application.getApplicationNo(),
                    () -> fraudRiskAgent.evaluate(application, documentResult)
            );
            fraudRiskResult.setId(store.nextFraudResultId());
            store.saveFraudResult(fraudRiskResult);

            RepaymentCapacityResult repaymentResult = runAgent(
                    applicationId,
                    "RepaymentCapacityAgent",
                    "DTI 与建议额度测算",
                    application.getLoanAmount().toPlainString(),
                    () -> repaymentCapacityAgent.evaluate(application, documentResult)
            );
            repaymentResult.setId(store.nextRepaymentResultId());
            store.saveRepaymentResult(repaymentResult);

            store.changeStatus(
                    application,
                    ApplicationStatus.DECISION_PENDING,
                    "合规决策 Agent 汇总审批结论",
                    "RISK_ANALYSIS_FINISHED",
                    OperatorType.AGENT,
                    "RepaymentCapacityAgent",
                    "风险与偿债能力分析完成"
            );

            ApprovalDecision decision = runAgent(
                    applicationId,
                    "ComplianceDecisionAgent",
                    "合规审批决策",
                    fraudRiskResult.getRiskLevel() + "/" + repaymentResult.getDti(),
                    () -> complianceDecisionAgent.decide(application, documentResult, fraudRiskResult, repaymentResult)
            );
            decision.setId(store.nextDecisionId());
            store.saveApprovalDecision(decision);
            addPolicyHit(applicationId, decision);
            finishByDecision(application, decision, fraudRiskResult);
        } catch (RuntimeException ex) {
            /*
             * 审批链路中 Agent 或工具失败时，第一版统一转人工复核。注意这里没有自动通过，
             * 因为金融审批里的降级兜底必须偏保守。
             */
            ManualReviewTicket ticket = createOrUpdateReviewTicket(
                    application,
                    "AUTO_FLOW_EXCEPTION",
                    "自动审批异常，已转人工复核：" + ex.getMessage()
            );
            store.saveReviewTicket(ticket);
            store.changeStatus(
                    application,
                    ApplicationStatus.MANUAL_REVIEW,
                    "自动审批异常，等待人工复核",
                    "AUTO_FLOW_EXCEPTION",
                    OperatorType.SYSTEM,
                    "orchestrator",
                    ex.getMessage()
            );
        }
    }

    private void finishByDecision(LoanApplication application, ApprovalDecision decision, FraudRiskResult fraudRiskResult) {
        if (decision.getDecisionResult() == DecisionResult.APPROVED) {
            store.changeStatus(
                    application,
                    ApplicationStatus.APPROVED,
                    "自动审批通过",
                    "AUTO_DECISION_APPROVED",
                    OperatorType.AGENT,
                    "ComplianceDecisionAgent",
                    decision.getDecisionExplanation()
            );
            return;
        }

        if (decision.getDecisionResult() == DecisionResult.REJECTED) {
            store.changeStatus(
                    application,
                    ApplicationStatus.REJECTED,
                    "自动审批拒绝",
                    "AUTO_DECISION_REJECTED",
                    OperatorType.AGENT,
                    "ComplianceDecisionAgent",
                    decision.getDecisionExplanation()
            );
            return;
        }

        ManualReviewTicket ticket = createOrUpdateReviewTicket(
                application,
                "AUTO_DECISION_MANUAL_REVIEW",
                "风险等级：" + fraudRiskResult.getRiskLevel() + "，" + decision.getDecisionExplanation()
        );
        store.saveReviewTicket(ticket);
        store.changeStatus(
                application,
                ApplicationStatus.MANUAL_REVIEW,
                "自动审批转人工复核",
                "AUTO_DECISION_MANUAL_REVIEW",
                OperatorType.AGENT,
                "ComplianceDecisionAgent",
                decision.getDecisionExplanation()
        );
    }

    private ManualReviewTicket createOrUpdateReviewTicket(LoanApplication application,
                                                          String triggerReason,
                                                          String riskSummary) {
        LocalDateTime now = LocalDateTime.now();
        ManualReviewTicket ticket = store.findReviewTicket(application.getApplicationId())
                .orElseGet(() -> new ManualReviewTicket(
                        store.nextTicketId(),
                        application.getApplicationId(),
                        "MR-" + application.getApplicationNo(),
                        ReviewStatus.PENDING,
                        "reviewer-001",
                        triggerReason,
                        riskSummary,
                        null,
                        null,
                        now,
                        now
                ));
        ticket.setReviewStatus(ReviewStatus.PENDING);
        ticket.setTriggerReason(triggerReason);
        ticket.setRiskSummary(riskSummary);
        ticket.setUpdatedAt(now);
        return ticket;
    }

    private void addPolicyHit(Long applicationId, ApprovalDecision decision) {
        PolicyHitRecord hitRecord = new PolicyHitRecord(
                store.nextPolicyHitId(),
                applicationId,
                "POLICY_CONSUMER_LOAN_001",
                "AUTO_APPROVAL_BOUNDARY",
                "消费贷自动审批边界策略",
                decision.getDecisionExplanation(),
                "LOCAL_RULE",
                LocalDateTime.now()
        );
        store.addPolicyHit(hitRecord);
    }

    private <T> T runAgent(Long applicationId,
                           String agentName,
                           String taskName,
                           String inputSummary,
                           Supplier<T> supplier) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            T output = supplier.get();
            LocalDateTime finishedAt = LocalDateTime.now();
            store.addAgentLog(new AgentTaskLog(
                    store.nextAgentLogId(),
                    applicationId,
                    agentName,
                    taskName,
                    inputSummary,
                    String.valueOf(output),
                    LogStatus.SUCCESS,
                    Duration.between(startedAt, finishedAt).toMillis(),
                    null,
                    startedAt,
                    finishedAt,
                    startedAt
            ));
            return output;
        } catch (RuntimeException ex) {
            LocalDateTime finishedAt = LocalDateTime.now();
            store.addAgentLog(new AgentTaskLog(
                    store.nextAgentLogId(),
                    applicationId,
                    agentName,
                    taskName,
                    inputSummary,
                    null,
                    LogStatus.FAILED,
                    Duration.between(startedAt, finishedAt).toMillis(),
                    ex.getMessage(),
                    startedAt,
                    finishedAt,
                    startedAt
            ));
            throw ex;
        }
    }
}
