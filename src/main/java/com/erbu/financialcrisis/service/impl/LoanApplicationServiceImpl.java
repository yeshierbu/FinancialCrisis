package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import com.erbu.financialcrisis.dto.request.CreateLoanApplicationRequest;
import com.erbu.financialcrisis.dto.response.LoanApplicationResponse;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import com.erbu.financialcrisis.service.LoanApplicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 申请服务实现。
 * 为了让骨架尽快跑起来，这里先使用内存存储模拟数据库，后续可平滑替换为 Repository 层。
 */
@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(100000L);
    private static final Map<Long, LoanApplication> STORE = new ConcurrentHashMap<>();

    private final AgentOrchestrationService agentOrchestrationService;

    public LoanApplicationServiceImpl(AgentOrchestrationService agentOrchestrationService) {
        this.agentOrchestrationService = agentOrchestrationService;
    }

    @Override
    public LoanApplicationResponse createApplication(CreateLoanApplicationRequest request) {
        LoanApplication application = new LoanApplication();
        application.setId(ID_GENERATOR.incrementAndGet());
        application.setApplicationNo("APP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        application.setProductCode(request.getProductCode());
        application.setApplicantName(request.getApplicantName());
        application.setIdCardNo(request.getIdCardNo());
        application.setMobile(request.getMobile());
        application.setLoanAmount(request.getLoanAmount());
        application.setLoanTerm(request.getLoanTerm());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setCurrentStep("申请已提交");
        application.setCreatedAt(LocalDateTime.now());
        STORE.put(application.getId(), application);

        // 真实项目里这里通常会改成异步投递任务，避免接口线程阻塞。
        agentOrchestrationService.startApprovalFlow(application);
        return toResponse(application);
    }

    @Override
    public LoanApplicationResponse getApplication(Long applicationId) {
        LoanApplication application = STORE.get(applicationId);
        if (application == null) {
            return null;
        }
        return toResponse(application);
    }

    private LoanApplicationResponse toResponse(LoanApplication application) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setApplicationId(application.getId());
        response.setApplicationNo(application.getApplicationNo());
        response.setStatus(application.getStatus());
        response.setCurrentStep(application.getCurrentStep());
        return response;
    }
}
