package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.StateTransitionLog;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.dto.response.StatusTimelineResponse;
import com.erbu.financialcrisis.service.LoanApplicationService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import com.erbu.financialcrisis.security.CurrentAccount;
import com.erbu.financialcrisis.common.BusinessException;

/**
 * 贷款申请业务服务实现。
 *
 * <p>负责申请主表的创建与查询。创建后快速返回 SUBMITTED，
 * 材料 OCR 成功后由 RabbitMQ 后台启动多 Agent 审批。
 * 数据由 ApprovalStore 统一持久化到 MySQL。</p>
 */
@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final ApprovalStore store;
    private final CurrentAccount currentAccount;
    public LoanApplicationServiceImpl(ApprovalStore store, CurrentAccount currentAccount) {
        this.store = store;
        this.currentAccount = currentAccount;
    }

    @Override
    @Transactional
    public LoanApplicationResponse createApplication(CreateLoanApplicationRequest request) {
        LocalDateTime now = LocalDateTime.now();

        LoanApplication application = new LoanApplication(
                null,
                generateApplicationNo(),
                request.getProductCode(),
                request.getApplicantName(),
                request.getIdCardNo(),
                request.getMobile(),
                request.getLoanAmount(),
                request.getLoanTerm(),
                request.getEmploymentType(),
                request.getCompanyName(),
                request.getWorkYears(),
                ApplicationStatus.SUBMITTED,
                "申请已提交，等待自动审批预处理",
                request.getChannelCode(),
                currentAccount.username(),
                now,
                now
        );

        store.saveApplication(application);
        store.changeStatus(
                application,
                ApplicationStatus.SUBMITTED,
                "申请已提交，等待自动审批预处理",
                "CREATE_APPLICATION",
                OperatorType.USER,
                request.getApplicantName(),
                "用户提交贷款申请"
        );

        return toResponse(application);
    }

    @Override
    public List<LoanApplicationResponse> listApplications() {
        List<LoanApplication> applications = currentAccount.privileged()
                ? store.listApplications() : store.listApplicationsOwnedBy(currentAccount.username());
        return applications.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public LoanApplicationResponse getApplication(Long applicationId) {
        return toResponse(getAuthorized(applicationId));
    }

    @Override
    public ApplicationStatusResponse getApplicationStatus(Long applicationId) {
        LoanApplication application = getAuthorized(applicationId);
        List<StatusTimelineResponse> timeline = store.listStateLogs(applicationId).stream()
                .map(this::toTimelineResponse)
                .toList();

        return new ApplicationStatusResponse(
                application.getApplicationId(),
                application.getStatus(),
                statusDesc(application.getStatus()),
                application.getUpdatedAt(),
                timeline
        );
    }

    private String generateApplicationNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "APP" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + suffix;
    }

    private LoanApplication getAuthorized(Long applicationId) {
        return currentAccount.privileged() ? store.getApplicationOrThrow(applicationId)
                : store.getOwnedApplicationOrThrow(applicationId, currentAccount.username());
    }

    private LoanApplicationResponse toResponse(LoanApplication application) {
        return new LoanApplicationResponse(
                application.getApplicationId(),
                application.getApplicationNo(),
                application.getProductCode(),
                application.getApplicantName(),
                application.getLoanAmount(),
                application.getLoanTerm(),
                application.getStatus(),
                application.getCurrentStep(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private StatusTimelineResponse toTimelineResponse(StateTransitionLog log) {
        return new StatusTimelineResponse(log.getToStatus(), log.getCreatedAt());
    }

    private String statusDesc(ApplicationStatus status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case SUBMITTED -> "申请已提交";
            case DOCUMENT_PENDING, MATERIAL_PENDING -> "等待补充材料";
            case OCR_PARSING -> "材料解析中";
            case EXTERNAL_VERIFYING -> "外部核验中";
            case RISK_ANALYZING -> "风险分析中";
            case DECISION_PENDING, DECISIONING -> "审批决策中";
            case MANUAL_REVIEW -> "人工复核中";
            case APPROVED -> "审批通过";
            case REJECTED -> "审批拒绝";
            case ARCHIVED -> "已归档";
        };
    }
}
