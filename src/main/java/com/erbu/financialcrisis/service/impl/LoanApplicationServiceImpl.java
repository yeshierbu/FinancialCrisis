package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.StateTransitionLog;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.domain.enums.OperatorType;
import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.ApplicationStatusResponse;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.dto.response.StatusTimelineResponse;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.service.LoanApplicationService;
import com.erbu.financialcrisis.store.ApprovalStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 贷款申请业务服务实现。
 *
 * <p>第一版按照指导书建议使用内存存储和同步编排，让申请主流程先跑通。
 * 后续接 MySQL 时，create/get/status 三个方法的边界可以保持不变，只把 store 调用替换为 Mapper。</p>
 */
@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final ApprovalStore store;
    private final AgentOrchestrationService agentOrchestrationService;

    public LoanApplicationServiceImpl(ApprovalStore store,
                                      AgentOrchestrationService agentOrchestrationService) {
        this.store = store;
        this.agentOrchestrationService = agentOrchestrationService;
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

        /*
         * 文档建议创建申请后立即启动审批编排。当前没有异步队列，所以采用同步调用：
         * 如果资料还没上传，编排会把申请推进到 DOCUMENT_PENDING，前端即可展示补件状态。
         */
        agentOrchestrationService.startApprovalFlow(application.getApplicationId());
        return toResponse(store.getApplicationOrThrow(application.getApplicationId()));
    }

    @Override
    public List<LoanApplicationResponse> listApplications() {
        return store.listApplications().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public LoanApplicationResponse getApplication(Long applicationId) {
        return toResponse(store.getApplicationOrThrow(applicationId));
    }

    @Override
    public ApplicationStatusResponse getApplicationStatus(Long applicationId) {
        LoanApplication application = store.getApplicationOrThrow(applicationId);
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
