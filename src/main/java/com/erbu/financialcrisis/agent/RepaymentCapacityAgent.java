package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.domain.entity.LoanApplication;
import org.springframework.stereotype.Component;

/**
 * 偿债能力测算 Agent。
 * 这里将来主要负责 DTI、收入稳定性、可支配收入和月供压力评估。
 */
@Component
public class RepaymentCapacityAgent {

    public void evaluate(LoanApplication application) {
        application.setCurrentStep("偿债能力评估完成");
    }
}
