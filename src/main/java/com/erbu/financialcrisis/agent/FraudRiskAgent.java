package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.enums.ApplicationStatus;
import org.springframework.stereotype.Component;

/**
 * 反欺诈风控 Agent。
 * 后续可以在这里整合黑名单、设备指纹、多头借贷和实名校验工具。
 */
@Component
public class FraudRiskAgent {

    public void evaluate(LoanApplication application) {
        application.setStatus(ApplicationStatus.RISK_ANALYZING);
        application.setCurrentStep("反欺诈风控分析中");
    }
}
