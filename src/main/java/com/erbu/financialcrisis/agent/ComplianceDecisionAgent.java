package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import org.springframework.stereotype.Component;

/**
 * 合规决策 Agent。
 * 这一层最终应该接规则引擎、RAG 检索和决策解释生成逻辑。
 */
@Component
public class ComplianceDecisionAgent {

    public void decide(LoanApplication application) {
        // 第一版先用固定结果占位，目的是把主链路跑通。
        application.setStatus(ApplicationStatus.MANUAL_REVIEW);
        application.setCurrentStep("已转人工复核");
    }
}
