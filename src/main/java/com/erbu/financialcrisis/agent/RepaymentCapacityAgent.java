package com.erbu.financialcrisis.agent;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 偿债能力测算 Agent。
 * 这里将来主要负责 DTI、收入稳定性、可支配收入和月供压力评估。
 */
@Component
public class RepaymentCapacityAgent {

    private static final BigDecimal MONTHLY_LIVING_COST = new BigDecimal("3000");

    /** 估算收入、DTI、可承受月供和推荐授信额度。 */
    public RepaymentCapacityResult evaluate(LoanApplication application, DocumentIntakeResult documentResult) {
        BigDecimal stableMonthlyIncome = estimateStableMonthlyIncome(application, documentResult);
        BigDecimal monthlyDebtPayment = calculateMonthlyPayment(application);
        BigDecimal dti = divide(monthlyDebtPayment, stableMonthlyIncome);
        BigDecimal foir = divide(monthlyDebtPayment.add(MONTHLY_LIVING_COST), stableMonthlyIncome);
        BigDecimal disposableIncome = stableMonthlyIncome.subtract(monthlyDebtPayment).subtract(MONTHLY_LIVING_COST);
        BigDecimal incomeStabilityScore = estimateIncomeStabilityScore(application);

        /*
         * 第一版用“收入的 45% 减生活成本”估算最大可承受月供。真实系统里可以把产品利率、
         * 家庭负债、地区生活成本和征信负债都纳入模型。
         */
        BigDecimal maxAffordableEmi = stableMonthlyIncome
                .multiply(new BigDecimal("0.45"))
                .subtract(MONTHLY_LIVING_COST)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal recommendedCreditLimit = maxAffordableEmi
                .multiply(BigDecimal.valueOf(application.getLoanTerm()))
                .multiply(new BigDecimal("0.80"))
                .setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        return new RepaymentCapacityResult(
                null,
                application.getApplicationId(),
                stableMonthlyIncome,
                monthlyDebtPayment,
                dti,
                foir,
                disposableIncome,
                incomeStabilityScore,
                maxAffordableEmi,
                recommendedCreditLimit,
                now,
                now
        );
    }

    private BigDecimal calculateMonthlyPayment(LoanApplication application) {
        return application.getLoanAmount()
                .divide(BigDecimal.valueOf(application.getLoanTerm()), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("1.08"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateStableMonthlyIncome(LoanApplication application, DocumentIntakeResult documentResult) {
        int workYears = application.getWorkYears() == null ? 0 : application.getWorkYears();
        BigDecimal baseIncome = switch (String.valueOf(application.getEmploymentType())) {
            case "FULL_TIME" -> new BigDecimal("12000");
            case "SELF_EMPLOYED" -> new BigDecimal("10000");
            default -> new BigDecimal("8000");
        };

        BigDecimal seniorityBonus = BigDecimal.valueOf(Math.min(workYears, 10)).multiply(new BigDecimal("800"));
        BigDecimal confidenceDiscount = documentResult.getParseConfidence().compareTo(new BigDecimal("0.80")) >= 0
                ? BigDecimal.ONE
                : new BigDecimal("0.80");
        return baseIncome.add(seniorityBonus)
                .multiply(confidenceDiscount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateIncomeStabilityScore(LoanApplication application) {
        int workYears = application.getWorkYears() == null ? 0 : application.getWorkYears();
        return BigDecimal.valueOf(Math.min(100, 60 + workYears * 8));
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
