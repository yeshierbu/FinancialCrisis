package com.erbu.financialcrisis.agent.tool;

import com.erbu.financialcrisis.agent.result.DocumentIntakeResult;
import com.erbu.financialcrisis.domain.entity.LoanApplication;
import com.erbu.financialcrisis.domain.entity.RepaymentCapacityResult;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 偿债能力计算器。使用可配置参数计算 DTI、收入稳定性、可支配收入和月供压力。
 */
@Component
public class RepaymentCapacityCalculator {
    private final BigDecimal monthlyLivingCost;
    private final BigDecimal fullTimeBaseIncome;
    private final BigDecimal selfEmployedBaseIncome;
    private final BigDecimal defaultBaseIncome;
    private final BigDecimal annualSeniorityBonus;
    private final BigDecimal maxPaymentRatio;
    private final BigDecimal creditSafetyFactor;
    private final BigDecimal annualRateFactor;

    public RepaymentCapacityCalculator(
            @Value("${approval.repayment.monthly-living-cost:3000}") BigDecimal monthlyLivingCost,
            @Value("${approval.repayment.full-time-base-income:12000}") BigDecimal fullTimeBaseIncome,
            @Value("${approval.repayment.self-employed-base-income:10000}") BigDecimal selfEmployedBaseIncome,
            @Value("${approval.repayment.default-base-income:8000}") BigDecimal defaultBaseIncome,
            @Value("${approval.repayment.annual-seniority-bonus:800}") BigDecimal annualSeniorityBonus,
            @Value("${approval.repayment.max-payment-ratio:0.45}") BigDecimal maxPaymentRatio,
            @Value("${approval.repayment.credit-safety-factor:0.80}") BigDecimal creditSafetyFactor,
            @Value("${approval.repayment.annual-rate-factor:1.08}") BigDecimal annualRateFactor) {
        this.monthlyLivingCost = monthlyLivingCost;
        this.fullTimeBaseIncome = fullTimeBaseIncome;
        this.selfEmployedBaseIncome = selfEmployedBaseIncome;
        this.defaultBaseIncome = defaultBaseIncome;
        this.annualSeniorityBonus = annualSeniorityBonus;
        this.maxPaymentRatio = maxPaymentRatio;
        this.creditSafetyFactor = creditSafetyFactor;
        this.annualRateFactor = annualRateFactor;
    }

    /** 估算收入、DTI、可承受月供和推荐授信额度。 */
    public RepaymentCapacityResult evaluate(LoanApplication application, DocumentIntakeResult documentResult) {
        BigDecimal stableMonthlyIncome = estimateStableMonthlyIncome(application, documentResult);
        BigDecimal monthlyDebtPayment = calculateMonthlyPayment(application);
        //DTI = 月还款额 / 稳定月收入
        BigDecimal dti = divide(monthlyDebtPayment, stableMonthlyIncome);
        //FOIR = (月还款额 + 生活成本) / 稳定月收入
        BigDecimal foir = divide(monthlyDebtPayment.add(monthlyLivingCost), stableMonthlyIncome);
      //可支配收入 = 稳定月收入 - 月还款额 - 每月最低生活成本
        BigDecimal disposableIncome = stableMonthlyIncome.subtract(monthlyDebtPayment).subtract(monthlyLivingCost);
     //收入稳定性分
        BigDecimal incomeStabilityScore = estimateIncomeStabilityScore(application);

        /*
         * 第一版用“收入的 45% 减生活成本”估算最大可承受月供。真实系统里可以把产品利率、
         * 家庭负债、地区生活成本和征信负债都纳入模型。
         */
        //最大可承受月供
        //最大可承受月供 = max(稳定月收入 × 45% - 每月最低生活成本, 0)
        BigDecimal maxAffordableEmi = stableMonthlyIncome
                .multiply(maxPaymentRatio)
                .subtract(monthlyLivingCost)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        //推荐授信额度 = 最大可承受月供 × 贷款期限 × 80%
        BigDecimal recommendedCreditLimit = maxAffordableEmi
                .multiply(BigDecimal.valueOf(application.getLoanTerm()))
                .multiply(creditSafetyFactor)
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

    /**
     * 估算月还款额度
     * @param application
     * @return
     */
    private BigDecimal calculateMonthlyPayment(LoanApplication application) {
        return application.getLoanAmount()
                .divide(BigDecimal.valueOf(application.getLoanTerm()), 2, RoundingMode.HALF_UP)
                .multiply(annualRateFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 估算稳定月收入
     * @param application
     * @param documentResult
     * @return
     */
    private BigDecimal estimateStableMonthlyIncome(LoanApplication application, DocumentIntakeResult documentResult) {
        int workYears = application.getWorkYears() == null ? 0 : application.getWorkYears();//工作年限
        BigDecimal baseIncome = switch (String.valueOf(application.getEmploymentType())) { //工作类型
            case "FULL_TIME" -> fullTimeBaseIncome;
            case "SELF_EMPLOYED" -> selfEmployedBaseIncome;
            default -> defaultBaseIncome;
        };

        BigDecimal seniorityBonus = BigDecimal.valueOf(Math.min(workYears, 10)).multiply(annualSeniorityBonus);//工龄奖励，最多奖励10年
        BigDecimal confidenceDiscount = documentResult.getParseConfidence().compareTo(new BigDecimal("0.80")) >= 0//如果材料置信度低于0.80，收入按0.8计算
                ? BigDecimal.ONE
                : new BigDecimal("0.80");
        //最终收入 = (基础收入 + 工龄奖励) * 置信度折扣
        return baseIncome.add(seniorityBonus)
                .multiply(confidenceDiscount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 根据申请人的工作年限估算收入稳定性分数
     * 收入稳定性分 = min(100, 60 + 工作年限 *8）
     * @param application
     * @return
     */
    private BigDecimal estimateIncomeStabilityScore(LoanApplication application) {
        int workYears = application.getWorkYears() == null ? 0 : application.getWorkYears();
        return BigDecimal.valueOf(Math.min(100, 60 + workYears * 8));
    }

    /**
     * 安全除法工具方法
     * @param numerator
     * @param denominator
     * @return
     */
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
