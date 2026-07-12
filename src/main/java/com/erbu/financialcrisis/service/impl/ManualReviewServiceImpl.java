package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.common.BusinessException;
import com.erbu.financialcrisis.domain.entity.ApprovalDecision;
import com.erbu.financialcrisis.domain.entity.FraudRiskResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.ManualReviewTicket;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.DecisionResult;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.domain.enums.ReviewStatus;
import com.erbu.financialcrisis.domain.enums.RiskLevel;
import com.erbu.financialcrisis.dto.request.ManualReviewRequest;
import com.erbu.financialcrisis.dto.request.ManualReviewPendingQueryRequest;
import com.erbu.financialcrisis.dto.response.ManualReviewDetailResponse;
import com.erbu.financialcrisis.dto.response.ManualReviewPendingResponse;
import com.erbu.financialcrisis.dto.response.ManualReviewResponse;
import com.erbu.financialcrisis.service.ManualReviewService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 人工复核业务服务实现。
 *
 * <p>人工复核是自动审批的安全阀。只要规则边界不确定、Agent 异常或风险处于中高区间，
 * 流程都会进入这里，由审核员给出最终通过或拒绝。</p>
 */
@Service
public class ManualReviewServiceImpl implements ManualReviewService {

    private final ApprovalStore store;

    public ManualReviewServiceImpl(ApprovalStore store) {
        this.store = store;
    }

    @Override
    public List<ManualReviewPendingResponse> queryPendingReviews(ManualReviewPendingQueryRequest request) {
        int pageNo = request.getPageNo() == null || request.getPageNo() < 1 ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        long offset = (long) (pageNo - 1) * pageSize;

        return store.listReviewTickets().stream()
                .filter(ticket -> ticket.getReviewStatus() == ReviewStatus.PENDING)
                .map(this::toPendingResponse)
                .filter(response -> request.getRiskLevel() == null || response.getRiskLevel() == request.getRiskLevel())
                .filter(response -> request.getProductCode() == null || request.getProductCode().equals(response.getProductCode()))
                .skip(offset)
                .limit(pageSize)
                .toList();
    }

    @Override
    public ManualReviewDetailResponse getReviewDetail(Long applicationId) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        ManualReviewTicket ticket = store.findReviewTicket(applicationId)
                .orElseThrow(() -> new BusinessException(4002, "人工复核工单不存在"));
        ApprovalDecision decision = store.findApprovalDecision(applicationId).orElse(null);

        return new ManualReviewDetailResponse(
                applicationId,
                ticket.getTicketNo(),
                application.getProductCode(),
                application.getApplicantName(),
                ticket.getRiskSummary(),
                ticket.getReviewStatus(),
                decision == null ? null : decision.getDecisionResult(),
                decision == null ? null : decision.getApprovedAmount(),
                decision == null ? null : decision.getRejectReasonCode(),
                decision == null ? null : decision.getDecisionExplanation()
        );
    }

    @Override
    @Transactional
    public ManualReviewResponse approve(Long applicationId, ManualReviewRequest request) {
        LoanApplication application = ensureManualReviewApplication(applicationId);
        ManualReviewTicket ticket = getOrCreateTicket(application);
        LocalDateTime now = LocalDateTime.now();

        ApprovalDecision decision = new ApprovalDecision(
                resolveDecisionId(applicationId),
                applicationId,
                DecisionResult.APPROVED,
                request.getApprovedAmount() == null ? defaultApprovedAmount(application) : request.getApprovedAmount(),
                request.getInterestRate() == null ? new BigDecimal("10.80") : request.getInterestRate(),
                request.getLoanTerm() == null ? application.getLoanTerm() : request.getLoanTerm(),
                null,
                "人工复核通过：" + request.getReviewComment(),
                "[\"POLICY_CONSUMER_LOAN_001#MANUAL_REVIEW\"]",
                "MANUAL_REVIEWER",
                now,
                now,
                now
        );
        store.saveApprovalDecision(decision);

        ticket.setReviewStatus(ReviewStatus.APPROVED);
        ticket.setReviewComment(request.getReviewComment());
        ticket.setReviewedAt(now);
        ticket.setUpdatedAt(now);
        store.saveReviewTicket(ticket);

        store.changeStatus(
                application,
                ApplicationStatus.APPROVED,
                "人工复核通过",
                "MANUAL_APPROVE",
                OperatorType.REVIEWER,
                ticket.getAssignedTo(),
                request.getReviewComment()
        );
        return new ManualReviewResponse(applicationId, ReviewStatus.APPROVED, DecisionResult.APPROVED, ApplicationStatus.APPROVED);
    }

    @Override
    @Transactional
    public ManualReviewResponse reject(Long applicationId, ManualReviewRequest request) {
        LoanApplication application = ensureManualReviewApplication(applicationId);
        ManualReviewTicket ticket = getOrCreateTicket(application);
        LocalDateTime now = LocalDateTime.now();

        ApprovalDecision decision = new ApprovalDecision(
                resolveDecisionId(applicationId),
                applicationId,
                DecisionResult.REJECTED,
                BigDecimal.ZERO,
                null,
                application.getLoanTerm(),
                request.getRejectReasonCode() == null ? "MANUAL_RISK_REJECT" : request.getRejectReasonCode(),
                "人工复核拒绝：" + request.getReviewComment(),
                "[\"POLICY_CONSUMER_LOAN_001#MANUAL_REVIEW\"]",
                "MANUAL_REVIEWER",
                now,
                now,
                now
        );
        store.saveApprovalDecision(decision);

        ticket.setReviewStatus(ReviewStatus.REJECTED);
        ticket.setReviewComment(request.getReviewComment());
        ticket.setReviewedAt(now);
        ticket.setUpdatedAt(now);
        store.saveReviewTicket(ticket);

        store.changeStatus(
                application,
                ApplicationStatus.REJECTED,
                "人工复核拒绝",
                "MANUAL_REJECT",
                OperatorType.REVIEWER,
                ticket.getAssignedTo(),
                request.getReviewComment()
        );
        return new ManualReviewResponse(applicationId, ReviewStatus.REJECTED, DecisionResult.REJECTED, ApplicationStatus.REJECTED);
    }

    private ManualReviewPendingResponse toPendingResponse(ManualReviewTicket ticket) {
        LoanApplication application = store.getApplicationOrThrow(ticket.getApplicationId());
        RiskLevel riskLevel = store.findFraudResult(ticket.getApplicationId())
                .map(FraudRiskResult::getRiskLevel)
                .orElse(RiskLevel.MEDIUM);
        return new ManualReviewPendingResponse(
                application.getApplicationId(),
                ticket.getTicketNo(),
                riskLevel,
                application.getProductCode(),
                application.getApplicantName(),
                ticket.getReviewStatus(),
                ticket.getAssignedTo(),
                ticket.getCreatedAt()
        );
    }

    private LoanApplication ensureManualReviewApplication(Long applicationId) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
        if (application.getStatus() != ApplicationStatus.MANUAL_REVIEW) {
            throw new BusinessException(4003, "只有人工复核状态允许执行该操作");
        }
        return application;
    }

    private ManualReviewTicket getOrCreateTicket(LoanApplication application) {
        return store.findReviewTicket(application.getApplicationId()).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            return new ManualReviewTicket(
                    null,
                    application.getApplicationId(),
                    "MR-" + application.getApplicationNo(),
                    ReviewStatus.PENDING,
                    "reviewer-001",
                    "MANUAL_REVIEW",
                    "自动审批转人工复核",
                    null,
                    null,
                    now,
                    now
            );
        });
    }

    private Long resolveDecisionId(Long applicationId) {
        return store.findApprovalDecision(applicationId)
                .map(ApprovalDecision::getId)
                .orElse(null);
    }

    private BigDecimal defaultApprovedAmount(LoanApplication application) {
        return store.findRepaymentResult(application.getApplicationId())
                .map(RepaymentCapacityResult::getRecommendedCreditLimit)
                .map(limit -> application.getLoanAmount().min(limit))
                .orElse(application.getLoanAmount());
    }
}
