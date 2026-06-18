package com.erbu.financialcrisis.service.impl;

import com.erbu.financialcrisis.agent.ComplianceDecisionAgent;
import com.erbu.financialcrisis.agent.DocumentIntakeAgent;
import com.erbu.financialcrisis.agent.FraudRiskAgent;
import com.erbu.financialcrisis.agent.RepaymentCapacityAgent;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.service.AgentOrchestrationService;
import org.springframework.stereotype.Service;

/**
 * Agent 编排实现。
 * 当前实现以串行示意为主，目的是把主流程骨架和职责边界表达清楚。
 */
@Service
public class AgentOrchestrationServiceImpl implements AgentOrchestrationService {

    private final DocumentIntakeAgent documentIntakeAgent;
    private final FraudRiskAgent fraudRiskAgent;
    private final RepaymentCapacityAgent repaymentCapacityAgent;
    private final ComplianceDecisionAgent complianceDecisionAgent;

    public AgentOrchestrationServiceImpl(DocumentIntakeAgent documentIntakeAgent,
                                         FraudRiskAgent fraudRiskAgent,
                                         RepaymentCapacityAgent repaymentCapacityAgent,
                                         ComplianceDecisionAgent complianceDecisionAgent) {
        this.documentIntakeAgent = documentIntakeAgent;
        this.fraudRiskAgent = fraudRiskAgent;
        this.repaymentCapacityAgent = repaymentCapacityAgent;
        this.complianceDecisionAgent = complianceDecisionAgent;
    }

    @Override
    public void startApprovalFlow(LoanApplication application) {
        // 真实场景里建议改成状态机 + 异步任务，而不是在同一个线程里跑完整条链路。
        documentIntakeAgent.collectAndParse(application);
        fraudRiskAgent.evaluate(application);
        repaymentCapacityAgent.evaluate(application);
        complianceDecisionAgent.decide(application);
    }
}
