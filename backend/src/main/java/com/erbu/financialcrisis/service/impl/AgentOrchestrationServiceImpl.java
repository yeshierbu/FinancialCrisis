package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.agent.tool.DocumentIntakeTool;
import com.erbu.financialcrisis.agent.tool.FraudRiskTool;
import com.erbu.financialcrisis.agent.tool.RepaymentCapacityCalculator;
import com.erbu.financialcrisis.agent.artifact.DocumentAnalysisReport;
import com.erbu.financialcrisis.agent.artifact.DecisionProposal;
import com.erbu.financialcrisis.agent.artifact.ReviewReport;
import com.erbu.financialcrisis.agent.artifact.RiskReport;
import com.erbu.financialcrisis.agent.collaboration.AgentFinding;
import com.erbu.financialcrisis.agent.collaboration.ApprovalCaseContext;
import com.erbu.financialcrisis.agent.guard.PolicyGuard;
import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.agent.worker.DecisionWorker;
import com.erbu.financialcrisis.agent.worker.DocumentAnalysisWorker;
import com.erbu.financialcrisis.agent.worker.ReviewWorker;
import com.erbu.financialcrisis.agent.worker.RiskWorker;
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
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ApprovalStore store;
    private final DocumentIntakeTool documentIntakeTool;
    private final FraudRiskTool fraudRiskTool;
    private final RepaymentCapacityCalculator repaymentCapacityCalculator;
    private final DocumentAnalysisWorker documentAnalysisWorker;
    private final RiskWorker riskWorker;
    private final ReviewWorker reviewWorker;
    private final DecisionWorker decisionWorker;
    private final PolicyGuard policyGuard;

    public AgentOrchestrationServiceImpl(ApprovalStore store,
                                         DocumentIntakeTool documentIntakeTool,
                                         FraudRiskTool fraudRiskTool,
                                         RepaymentCapacityCalculator repaymentCapacityCalculator,
                                         DocumentAnalysisWorker documentAnalysisWorker,
                                         RiskWorker riskWorker,
                                         ReviewWorker reviewWorker,
                                         DecisionWorker decisionWorker,
                                         PolicyGuard policyGuard) {
        this.store = store;
        this.documentIntakeTool = documentIntakeTool;
        this.fraudRiskTool = fraudRiskTool;
        this.repaymentCapacityCalculator = repaymentCapacityCalculator;
        this.documentAnalysisWorker = documentAnalysisWorker;
        this.riskWorker = riskWorker;
        this.reviewWorker = reviewWorker;
        this.decisionWorker = decisionWorker;
        this.policyGuard = policyGuard;
    }

    /**
     * 执行一次完整审批：材料检查、确定性工具测算、风险分析、独立复核、LLM 决策和安全护栏。
     * 技术异常向上抛给消息消费者，由 RabbitMQ 延迟重试；重试耗尽后再转人工审核。
     */
    @Override
    @Transactional
    public void startApprovalFlow(Long applicationId) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        /**
         * 创建本次审判流程的共享上下文，绑定当前的applicationId,后续各个Agent可以往这个上下文区域写发现
         */
        ApprovalCaseContext context = new ApprovalCaseContext(applicationId);

        /**
         *
         */
        try {
            store.changeStatus(
                    application,
                    ApplicationStatus.OCR_PARSING,
                    "材料采集工具校验申请材料",
                    "START_APPROVAL_FLOW",
                    OperatorType.SYSTEM,
                    "orchestrator",
                    "启动同步审批编排流程"
            );

            List<UploadedDocument> documents = store.listDocuments(applicationId);
            DocumentIntakeResult documentResult = runAgent(
                    applicationId,
                    "DocumentIntakeTool",
                    "材料完整性与百度千帆 OCR 结果校验",
                    "documents=" + documents.size(),
                    () -> documentIntakeTool.collectAndParse(application, documents)
            );
            documents.forEach(store::updateDocument);
            context.addFinding(new AgentFinding(
                    "DocumentIntakeTool",
                    "材料采集",
                    documentResult.getSummary(),
                    documentResult.getParseConfidence(),
                    documentResult.getMissingDocuments().stream().map(String::valueOf).toList(),
                    Boolean.TRUE.equals(documentResult.getNeedSupplement()) ? "SUPPLEMENT" : "CONTINUE"
            ));

            if (Boolean.TRUE.equals(documentResult.getNeedSupplement())) {
                store.changeStatus(
                        application,
                        ApplicationStatus.DOCUMENT_PENDING,
                        "等待用户补充材料：" + documentResult.getMissingDocuments(),
                        "DOCUMENT_SUPPLEMENT_REQUIRED",
                        OperatorType.AGENT,
                        "DocumentIntakeTool",
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
                    "DocumentIntakeTool",
                    documentResult.getSummary()
            );

            DocumentAnalysisReport documentAnalysis = runAgent(
                    applicationId,
                    "DocumentAnalysisWorker",
                    "LLM 理解 OCR 文本与材料一致性",
                    "documents=" + documents.size(),
                    () -> documentAnalysisWorker.analyze(application, documents, documentResult)
            );
            context.addFinding(new AgentFinding(
                    "DocumentAnalysisWorker", "LLM 材料分析", documentAnalysis.getSummary(),
                    documentAnalysis.getConfidence(),
                    documentAnalysis.getEvidence() == null ? List.of() : documentAnalysis.getEvidence(),
                    documentAnalysis.getRecommendedAction()
            ));

            FraudRiskResult fraudRiskResult = runAgent(
                    applicationId,
                    "FraudRiskTool",
                    "反欺诈规则评估",
                    application.getApplicationNo(),
                    () -> fraudRiskTool.evaluate(application, documentResult)
            );
            store.saveFraudResult(fraudRiskResult);
            context.addFinding(new AgentFinding(
                    "FraudRiskTool",
                    "反欺诈风控",
                    "风险等级：" + fraudRiskResult.getRiskLevel() + "，建议动作：" + fraudRiskResult.getSuggestedAction(),
                    fraudRiskResult.getRiskScore().divide(new java.math.BigDecimal("100")),
                    List.of(fraudRiskResult.getRiskTagsJson(), fraudRiskResult.getRuleHitsJson()),
                    fraudRiskResult.getSuggestedAction()
            ));

            RepaymentCapacityResult repaymentResult = runAgent(
                    applicationId,
                    "RepaymentCapacityCalculator",
                    "DTI 与建议额度测算",
                    application.getLoanAmount().toPlainString(),
                    () -> repaymentCapacityCalculator.evaluate(application, documentResult)
            );
            store.saveRepaymentResult(repaymentResult);
            context.addFinding(new AgentFinding(
                    "RepaymentCapacityCalculator",
                    "偿债能力测算",
                    "DTI：" + repaymentResult.getDti() + "，建议额度：" + repaymentResult.getRecommendedCreditLimit(),
                    repaymentResult.getIncomeStabilityScore().divide(new java.math.BigDecimal("100")),
                    List.of("稳定月收入：" + repaymentResult.getStableMonthlyIncome(),
                            "月供：" + repaymentResult.getMonthlyDebtPayment(),
                            "可支配收入：" + repaymentResult.getDisposableIncome()),
                    repaymentResult.getDti().compareTo(new java.math.BigDecimal("0.50")) > 0
                            ? "MANUAL_REVIEW" : "CONTINUE"
            ));

            store.changeStatus(
                    application,
                    ApplicationStatus.DECISION_PENDING,
                    "协作审查与合规决策 Agent 汇总审批结论",
                    "RISK_ANALYSIS_FINISHED",
                    OperatorType.AGENT,
                    "RepaymentCapacityCalculator",
                    "风险与偿债能力分析完成"
            );

            RiskReport riskReport = runAgent(
                    applicationId,
                    "RiskWorker",
                    "LLM 综合风险分析与政策检索",
                    "findings=" + context.getFindings().size(),
                    () -> riskWorker.analyze(application, fraudRiskResult, repaymentResult,
                            context.getFindings().stream().map(AgentFinding::getConclusion).toList(), List.of())
            );
            context.addFinding(new AgentFinding(
                    "RiskWorker", "LLM 综合风险分析", riskReport.getSummary(),
                    riskReport.getConfidence(), riskReport.getEvidence(), riskReport.getRecommendedAction()
            ));

            RiskReport initialRiskReport = riskReport;
            ReviewReport reviewReport = runAgent(
                    applicationId,
                    "ReviewWorker",
                    "独立 LLM 证据与结论复核",
                    "riskAction=" + initialRiskReport.getRecommendedAction(),
                    () -> reviewWorker.review(initialRiskReport, fraudRiskResult, repaymentResult)
            );

            // 复核不通过时只允许一次定向返工，防止 Agent 互相否定形成无限循环。
            if (!Boolean.TRUE.equals(reviewReport.getAccepted())) {
                List<String> instructions = reviewReport.getRevisionInstructions() == null
                        ? List.of("根据复核意见重新核对事实和证据") : reviewReport.getRevisionInstructions();
                riskReport = runAgent(applicationId, "RiskWorker", "按复核意见返工一次",
                        "issues=" + instructions.size(),
                        () -> riskWorker.analyze(application, fraudRiskResult, repaymentResult,
                                context.getFindings().stream().map(AgentFinding::getConclusion).toList(), instructions));
                RiskReport revisedRiskReport = riskReport;
                reviewReport = runAgent(applicationId, "ReviewWorker", "复核返工后的风险报告",
                        "revision=1", () -> reviewWorker.review(revisedRiskReport, fraudRiskResult, repaymentResult));
            }

            context.addFinding(new AgentFinding(
                    "ReviewWorker", "独立复核", reviewReport.getSummary(),
                    reviewReport.getConfidence(), reviewReport.getContradictions(), reviewReport.getRecommendedAction()
            ));

            RiskReport finalRiskReport = riskReport;
            ReviewReport finalReviewReport = reviewReport;
            DecisionProposal proposal = runAgent(
                    applicationId,
                    "DecisionWorker",
                    "LLM 生成最终审批建议",
                    "risk=" + finalRiskReport.getRecommendedAction()
                            + "/review=" + finalReviewReport.getRecommendedAction(),
                    () -> decisionWorker.decide(application, finalRiskReport, finalReviewReport, repaymentResult)
            );
            ApprovalDecision decision = runAgent(applicationId, "PolicyGuard", "确定性安全护栏",
                    proposal.getDecision(), () -> policyGuard.validate(application, documentResult,
                            fraudRiskResult, repaymentResult, finalReviewReport, proposal));
            store.saveApprovalDecision(decision);
            addPolicyHit(applicationId, decision);
            finishByDecision(application, decision, fraudRiskResult);
        } catch (RuntimeException ex) {
            // 必须抛出异常使当前事务回滚，否则消费者无法判断本次审批是否需要重试。
            throw new IllegalStateException("自动审批执行失败：" + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void moveToManualReview(Long applicationId, String reason) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        if (application.getStatus() == ApplicationStatus.APPROVED
                || application.getStatus() == ApplicationStatus.REJECTED
                || application.getStatus() == ApplicationStatus.ARCHIVED) {
            return;
        }
        String safeReason = reason == null || reason.isBlank() ? "自动审批多次执行失败" : reason;
        ManualReviewTicket ticket = createOrUpdateReviewTicket(
                application, "AUTO_FLOW_RETRY_EXHAUSTED", safeReason);
        store.saveReviewTicket(ticket);
        store.changeStatus(
                application,
                ApplicationStatus.MANUAL_REVIEW,
                "自动审批重试耗尽，等待人工复核",
                "AUTO_FLOW_RETRY_EXHAUSTED",
                OperatorType.SYSTEM,
                "approval-consumer",
                safeReason
        );
    }

    private void finishByDecision(LoanApplication application, ApprovalDecision decision, FraudRiskResult fraudRiskResult) {
        if (decision.getDecisionResult() == DecisionResult.APPROVED) {
            store.changeStatus(
                    application,
                    ApplicationStatus.APPROVED,
                    "自动审批通过",
                    "AUTO_DECISION_APPROVED",
                    OperatorType.AGENT,
                    "DecisionWorker",
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
                    "DecisionWorker",
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
                "DecisionWorker",
                decision.getDecisionExplanation()
        );
    }

    private ManualReviewTicket createOrUpdateReviewTicket(LoanApplication application,
                                                          String triggerReason,
                                                          String riskSummary) {
        LocalDateTime now = LocalDateTime.now();
        ManualReviewTicket ticket = store.findReviewTicket(application.getApplicationId())
                .orElseGet(() -> new ManualReviewTicket(
                        null,
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
                null,
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

    /** 统一执行 Agent，并记录成功、失败和耗时信息。 */
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
                    null,
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
                    null,
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
