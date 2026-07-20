package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.agent.artifact.*;
import com.erbu.financialcrisis.agent.guard.PolicyGuard;
import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.agent.tool.DocumentIntakeTool;
import com.erbu.financialcrisis.agent.tool.FraudRiskTool;
import com.erbu.financialcrisis.agent.tool.RepaymentCapacityCalculator;
import com.erbu.financialcrisis.agent.worker.*;
import com.erbu.financialcrisis.domain.entity.*;
import com.erbu.financialcrisis.domain.enums.*;
import com.erbu.financialcrisis.service.ApprovalCheckpointService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/** Executes one and only one durable approval step per invocation. */
@Service
public class ApprovalSingleStepExecutor {
    private final ApprovalStore store;
    private final ApprovalCheckpointService checkpoints;
    private final DocumentIntakeTool intakeTool;
    private final DocumentAnalysisWorker documentWorker;
    private final FraudRiskTool fraudTool;
    private final RepaymentCapacityCalculator repaymentTool;
    private final RiskWorker riskWorker;
    private final ReviewWorker reviewWorker;
    private final DecisionWorker decisionWorker;
    private final PolicyGuard policyGuard;

    public ApprovalSingleStepExecutor(ApprovalStore store, ApprovalCheckpointService checkpoints,
                                      DocumentIntakeTool intakeTool, DocumentAnalysisWorker documentWorker,
                                      FraudRiskTool fraudTool, RepaymentCapacityCalculator repaymentTool,
                                      RiskWorker riskWorker, ReviewWorker reviewWorker,
                                      DecisionWorker decisionWorker, PolicyGuard policyGuard) {
        this.store = store; this.checkpoints = checkpoints; this.intakeTool = intakeTool;
        this.documentWorker = documentWorker; this.fraudTool = fraudTool; this.repaymentTool = repaymentTool;
        this.riskWorker = riskWorker; this.reviewWorker = reviewWorker;
        this.decisionWorker = decisionWorker; this.policyGuard = policyGuard;
    }

    public ApprovalStep execute(Long applicationId, ApprovalStep step) {
        LoanApplication app = store.getApplicationOrThrow(applicationId);
        if (isTerminal(app.getStatus())) return null;
        return switch (step) {
            case DOCUMENT_INTAKE -> intake(app);
            case DOCUMENT_ANALYSIS -> documentAnalysis(app);
            case FRAUD_ASSESSMENT -> fraud(app);
            case REPAYMENT_ASSESSMENT -> repayment(app);
            case RISK_ANALYSIS -> risk(app, false);
            case INDEPENDENT_REVIEW -> review(app, false);
            case RISK_REVISION -> risk(app, true);
            case REVISION_REVIEW -> review(app, true);
            case FINAL_DECISION -> decision(app);
            case POLICY_GUARD -> guard(app);
        };
    }

    private ApprovalStep intake(LoanApplication app) {
        if (app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.DOCUMENT_PENDING
                || app.getStatus() == ApplicationStatus.MATERIAL_PENDING) {
            store.changeStatus(app, ApplicationStatus.OCR_PARSING, "材料采集工具校验申请材料",
                    "STEP_DOCUMENT_INTAKE", OperatorType.SYSTEM, "orchestrator", "开始材料采集步骤");
        }
        List<UploadedDocument> documents = store.listDocuments(app.getApplicationId());
        DocumentIntakeResult result = checkpoints.restoreOrExecute(app.getApplicationId(), "DOCUMENT_INTAKE",
                DocumentIntakeResult.class, () -> run(app.getApplicationId(), "DocumentIntakeTool",
                        () -> intakeTool.collectAndParse(app, documents)));
        documents.forEach(store::updateDocument);
        if (Boolean.TRUE.equals(result.getNeedSupplement())) {
            store.changeStatus(app, ApplicationStatus.DOCUMENT_PENDING, "等待用户补充材料：" + result.getMissingDocuments(),
                    "DOCUMENT_SUPPLEMENT_REQUIRED", OperatorType.AGENT, "DocumentIntakeTool", result.getSummary());
            return null;
        }
        if (app.getStatus() == ApplicationStatus.OCR_PARSING) {
            store.changeStatus(app, ApplicationStatus.RISK_ANALYZING, "材料分析中", "DOCUMENT_INTAKE_FINISHED",
                    OperatorType.AGENT, "DocumentIntakeTool", result.getSummary());
        }
        return ApprovalStep.DOCUMENT_ANALYSIS;
    }

    private ApprovalStep documentAnalysis(LoanApplication app) {
        DocumentIntakeResult intake = requireIntake(app);
        List<UploadedDocument> documents = store.listDocuments(app.getApplicationId());
        checkpoints.restoreOrExecute(app.getApplicationId(), "DOCUMENT_ANALYSIS", DocumentAnalysisReport.class,
                () -> run(app.getApplicationId(), "DocumentAnalysisWorker",
                        () -> documentWorker.analyze(app, documents, intake)));
        return ApprovalStep.FRAUD_ASSESSMENT;
    }

    private ApprovalStep fraud(LoanApplication app) {
        if (store.findFraudResult(app.getApplicationId()).isEmpty()) {
            FraudRiskResult result = run(app.getApplicationId(), "FraudRiskTool",
                    () -> fraudTool.evaluate(app, requireIntake(app)));
            store.saveFraudResult(result);
        }
        return ApprovalStep.REPAYMENT_ASSESSMENT;
    }

    private ApprovalStep repayment(LoanApplication app) {
        if (store.findRepaymentResult(app.getApplicationId()).isEmpty()) {
            RepaymentCapacityResult result = run(app.getApplicationId(), "RepaymentCapacityCalculator",
                    () -> repaymentTool.evaluate(app, requireIntake(app)));
            store.saveRepaymentResult(result);
        }
        return ApprovalStep.RISK_ANALYSIS;
    }

    private ApprovalStep risk(LoanApplication app, boolean revision) {
        FraudRiskResult fraud = requireFraud(app);
        RepaymentCapacityResult repayment = requireRepayment(app);
        DocumentAnalysisReport document = checkpoints.require(app.getApplicationId(), "DOCUMENT_ANALYSIS", DocumentAnalysisReport.class);
        if (!revision && app.getStatus() == ApplicationStatus.RISK_ANALYZING) {
            store.changeStatus(app, ApplicationStatus.DECISION_PENDING, "综合风险分析与决策中",
                    "RISK_TO_DECISION", OperatorType.AGENT, "RiskWorker", "确定性分析完成");
        }
        List<String> instructions = revision
                ? safe(checkpoints.require(app.getApplicationId(), "INDEPENDENT_REVIEW", ReviewReport.class).getRevisionInstructions())
                : List.of();
        String key = revision ? "RISK_REVISION" : "RISK_ANALYSIS";
        checkpoints.restoreOrExecute(app.getApplicationId(), key, RiskReport.class,
                () -> run(app.getApplicationId(), "RiskWorker", () -> riskWorker.analyze(app, fraud, repayment,
                        List.of(document.getSummary()), instructions)));
        return revision ? ApprovalStep.REVISION_REVIEW : ApprovalStep.INDEPENDENT_REVIEW;
    }

    private ApprovalStep review(LoanApplication app, boolean revision) {
        RiskReport risk = checkpoints.require(app.getApplicationId(), revision ? "RISK_REVISION" : "RISK_ANALYSIS", RiskReport.class);
        String key = revision ? "REVISION_REVIEW" : "INDEPENDENT_REVIEW";
        ReviewReport review = checkpoints.restoreOrExecute(app.getApplicationId(), key, ReviewReport.class,
                () -> run(app.getApplicationId(), "ReviewWorker",
                        () -> reviewWorker.review(risk, requireFraud(app), requireRepayment(app))));
        if (!revision && !Boolean.TRUE.equals(review.getAccepted())) return ApprovalStep.RISK_REVISION;
        return ApprovalStep.FINAL_DECISION;
    }

    private ApprovalStep decision(LoanApplication app) {
        ReviewReport initial = checkpoints.require(app.getApplicationId(), "INDEPENDENT_REVIEW", ReviewReport.class);
        boolean revised = !Boolean.TRUE.equals(initial.getAccepted());
        RiskReport risk = checkpoints.require(app.getApplicationId(), revised ? "RISK_REVISION" : "RISK_ANALYSIS", RiskReport.class);
        ReviewReport review = checkpoints.require(app.getApplicationId(), revised ? "REVISION_REVIEW" : "INDEPENDENT_REVIEW", ReviewReport.class);
        checkpoints.restoreOrExecute(app.getApplicationId(), "FINAL_DECISION", DecisionProposal.class,
                () -> run(app.getApplicationId(), "DecisionWorker",
                        () -> decisionWorker.decide(app, risk, review, requireRepayment(app))));
        return ApprovalStep.POLICY_GUARD;
    }

    private ApprovalStep guard(LoanApplication app) {
        ReviewReport initial = checkpoints.require(app.getApplicationId(), "INDEPENDENT_REVIEW", ReviewReport.class);
        boolean revised = !Boolean.TRUE.equals(initial.getAccepted());
        ReviewReport review = checkpoints.require(app.getApplicationId(), revised ? "REVISION_REVIEW" : "INDEPENDENT_REVIEW", ReviewReport.class);
        DecisionProposal proposal = checkpoints.require(app.getApplicationId(), "FINAL_DECISION", DecisionProposal.class);
        ApprovalDecision decision = run(app.getApplicationId(), "PolicyGuard", () -> policyGuard.validate(app,
                requireIntake(app), requireFraud(app), requireRepayment(app), review, proposal));
        store.saveApprovalDecision(decision);
        finish(app, decision, requireFraud(app));
        return null;
    }

    private void finish(LoanApplication app, ApprovalDecision decision, FraudRiskResult fraud) {
        if (decision.getDecisionResult() == DecisionResult.APPROVED) {
            store.changeStatus(app, ApplicationStatus.APPROVED, "自动审批通过", "AUTO_DECISION_APPROVED",
                    OperatorType.AGENT, "PolicyGuard", decision.getDecisionExplanation()); return;
        }
        if (decision.getDecisionResult() == DecisionResult.REJECTED) {
            store.changeStatus(app, ApplicationStatus.REJECTED, "自动审批拒绝", "AUTO_DECISION_REJECTED",
                    OperatorType.AGENT, "PolicyGuard", decision.getDecisionExplanation()); return;
        }
        LocalDateTime now = LocalDateTime.now();
        ManualReviewTicket ticket = store.findReviewTicket(app.getApplicationId()).orElseGet(() ->
                new ManualReviewTicket(null, app.getApplicationId(), "MR-" + app.getApplicationNo(), ReviewStatus.PENDING,
                        "reviewer", "AUTO_DECISION_MANUAL_REVIEW", "风险等级：" + fraud.getRiskLevel(), null, null, now, now));
        store.saveReviewTicket(ticket);
        store.changeStatus(app, ApplicationStatus.MANUAL_REVIEW, "自动审批转人工复核", "AUTO_DECISION_MANUAL_REVIEW",
                OperatorType.AGENT, "PolicyGuard", decision.getDecisionExplanation());
    }

    private DocumentIntakeResult requireIntake(LoanApplication app) {
        return checkpoints.require(app.getApplicationId(), "DOCUMENT_INTAKE", DocumentIntakeResult.class);
    }
    private FraudRiskResult requireFraud(LoanApplication app) {
        return store.findFraudResult(app.getApplicationId()).orElseThrow(() -> new IllegalStateException("缺少反欺诈结果"));
    }
    private RepaymentCapacityResult requireRepayment(LoanApplication app) {
        return store.findRepaymentResult(app.getApplicationId()).orElseThrow(() -> new IllegalStateException("缺少偿债能力结果"));
    }
    private boolean isTerminal(ApplicationStatus status) {
        return status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED
                || status == ApplicationStatus.ARCHIVED || status == ApplicationStatus.MANUAL_REVIEW;
    }
    private List<String> safe(List<String> value) { return value == null ? List.of() : value; }

    private <T> T run(Long applicationId, String agentName, Supplier<T> work) {
        LocalDateTime start = LocalDateTime.now();
        try {
            T result = work.get();
            store.addAgentLog(new AgentTaskLog(null, applicationId, agentName, agentName, "step-message",
                    String.valueOf(result), LogStatus.SUCCESS, Duration.between(start, LocalDateTime.now()).toMillis(),
                    null, start, LocalDateTime.now(), start));
            return result;
        } catch (RuntimeException ex) {
            store.addAgentLog(new AgentTaskLog(null, applicationId, agentName, agentName, "step-message", null,
                    LogStatus.FAILED, Duration.between(start, LocalDateTime.now()).toMillis(), ex.getMessage(),
                    start, LocalDateTime.now(), start));
            throw ex;
        }
    }
}
