package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.GoalPlanResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentProjectionResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;

@Service
public class InvestmentRecommendationService {

    public static final int SCALE = 2;
    public static final BigDecimal DEFAULT_ALLOCATION_PERCENT = new BigDecimal("30.00");
    public static final BigDecimal REDUCED_ALLOCATION_PERCENT = new BigDecimal("15.00");

    public static final BigDecimal CONSERVATIVE_EQUITY = new BigDecimal("30.00");
    public static final BigDecimal CONSERVATIVE_DEBT = new BigDecimal("50.00");
    public static final BigDecimal CONSERVATIVE_GOLD = new BigDecimal("20.00");

    public static final BigDecimal MODERATE_EQUITY = new BigDecimal("60.00");
    public static final BigDecimal MODERATE_DEBT = new BigDecimal("30.00");
    public static final BigDecimal MODERATE_GOLD = new BigDecimal("10.00");

    public static final BigDecimal AGGRESSIVE_EQUITY = new BigDecimal("80.00");
    public static final BigDecimal AGGRESSIVE_DEBT = new BigDecimal("10.00");
    public static final BigDecimal AGGRESSIVE_GOLD = new BigDecimal("10.00");

    public static final BigDecimal CONSERVATIVE_RETURN = new BigDecimal("7.00");
    public static final BigDecimal MODERATE_RETURN = new BigDecimal("10.00");
    public static final BigDecimal AGGRESSIVE_RETURN = new BigDecimal("12.00");
    public static final int DEFAULT_PROJECTION_YEARS = 10;
    public static final int MAX_PROJECTION_YEARS = 50;

    private final FinancialSummaryService financialSummaryService;
    private final EmergencyFundService emergencyFundService;
    private final FinancialGoalService financialGoalService;
    private final FinancialGoalRepository financialGoalRepository;
    private final InvestmentProfileRepository investmentProfileRepository;

    public InvestmentRecommendationService(
            FinancialSummaryService financialSummaryService,
            EmergencyFundService emergencyFundService,
            FinancialGoalService financialGoalService,
            FinancialGoalRepository financialGoalRepository,
            InvestmentProfileRepository investmentProfileRepository) {
        this.financialSummaryService = financialSummaryService;
        this.emergencyFundService = emergencyFundService;
        this.financialGoalService = financialGoalService;
        this.financialGoalRepository = financialGoalRepository;
        this.investmentProfileRepository = investmentProfileRepository;
    }

    @Transactional(readOnly = true)
    public InvestmentRecommendationResponse recommend(Long userId) {
        return recommend(userId, null);
    }

    @Transactional(readOnly = true)
    public InvestmentRecommendationResponse recommend(Long userId, Long goalId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        // 1. Obtain disposable income from FinancialSummaryService
        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, null, null);
        BigDecimal disposableIncome = (summary != null && summary.disposableIncome() != null)
                ? summary.disposableIncome()
                : BigDecimal.ZERO;
        BigDecimal positiveDisposable = disposableIncome.max(BigDecimal.ZERO);

        // 2. Obtain emergency fund status from EmergencyFundService
        EmergencyFundPlanResponse efPlan = null;
        try {
            efPlan = emergencyFundService.calculate(userId);
        } catch (Exception ignored) {
        }
        BigDecimal efRemaining = (efPlan != null && efPlan.remainingAmountNeeded() != null)
                ? efPlan.remainingAmountNeeded()
                : BigDecimal.ZERO;
        boolean emergencyFundReady = efRemaining.compareTo(BigDecimal.ZERO) <= 0;

        // 3. Determine investment allocation percentage
        // Rule 1 & Rule 2: If emergency fund is incomplete, allocate conservative 15%; otherwise 30%.
        BigDecimal investmentAllocationPercentage = emergencyFundReady
                ? DEFAULT_ALLOCATION_PERCENT
                : REDUCED_ALLOCATION_PERCENT;

        // Rule 2 & Step 3: SIP recommendation = positiveDisposable * investmentAllocationPercentage
        BigDecimal recommendedMonthlyInvestment;
        if (positiveDisposable.compareTo(BigDecimal.ZERO) <= 0) {
            recommendedMonthlyInvestment = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        } else {
            recommendedMonthlyInvestment = positiveDisposable
                    .multiply(investmentAllocationPercentage)
                    .movePointLeft(2)
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }

        // 4. Determine risk preference
        InvestmentRiskPreference riskPreference = investmentProfileRepository.findByUserId(userId)
                .map(InvestmentProfile::getRiskPreference)
                .orElse(InvestmentRiskPreference.MODERATE);

        // 5. Determine asset allocation percentages
        BigDecimal equityPct;
        BigDecimal debtPct;
        BigDecimal goldPct;
        switch (riskPreference) {
            case CONSERVATIVE -> {
                equityPct = CONSERVATIVE_EQUITY;
                debtPct = CONSERVATIVE_DEBT;
                goldPct = CONSERVATIVE_GOLD;
            }
            case AGGRESSIVE -> {
                equityPct = AGGRESSIVE_EQUITY;
                debtPct = AGGRESSIVE_DEBT;
                goldPct = AGGRESSIVE_GOLD;
            }
            default -> {
                equityPct = MODERATE_EQUITY;
                debtPct = MODERATE_DEBT;
                goldPct = MODERATE_GOLD;
            }
        }

        // M5.7: Assess diversification across the three asset classes
        String diversificationStatus = assessDiversificationStatus(equityPct, debtPct, goldPct);
        boolean diversified = "DIVERSIFIED".equals(diversificationStatus);
        String diversificationMessage = buildDiversificationMessage(
                riskPreference,
                equityPct.setScale(SCALE, RoundingMode.HALF_UP),
                debtPct.setScale(SCALE, RoundingMode.HALF_UP),
                goldPct.setScale(SCALE, RoundingMode.HALF_UP),
                diversificationStatus);

        // Step 4: Calculate allocation amounts, ensuring exact sum
        BigDecimal equityAmount = recommendedMonthlyInvestment.multiply(equityPct).movePointLeft(2)
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal debtAmount = recommendedMonthlyInvestment.multiply(debtPct).movePointLeft(2)
                .setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal goldAmount = recommendedMonthlyInvestment.subtract(equityAmount).subtract(debtAmount)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 6. Goal-aware planning
        GoalPlanResponse goalPlan = null;
        if (goalId != null) {
            goalPlan = financialGoalService.getPlan(goalId, userId);
        } else {
            List<FinancialGoal> userGoals = financialGoalRepository.findByUserIdOrderByTargetDateAsc(userId);
            for (FinancialGoal g : userGoals) {
                if (g.getCurrentAmount() == null || g.getTargetAmount() == null
                        || g.getCurrentAmount().compareTo(g.getTargetAmount()) < 0) {
                    goalPlan = financialGoalService.getPlan(g.getId(), userId);
                    break;
                }
            }
        }

        String goalName = null;
        BigDecimal goalTargetAmount = null;
        BigDecimal goalCurrentAmount = null;
        BigDecimal goalRemainingAmount = null;
        BigDecimal goalRequiredMonthlyContribution = null;
        Boolean goalContributionCovered = null;

        List<String> warnings = new ArrayList<>();
        StringBuilder reason = new StringBuilder();

        if (goalPlan != null) {
            goalName = goalPlan.goalName();
            goalTargetAmount = goalPlan.targetAmount();
            goalCurrentAmount = goalPlan.currentAmount();
            goalRemainingAmount = goalPlan.remainingAmount();
            goalRequiredMonthlyContribution = goalPlan.requiredMonthlyContribution();
            goalContributionCovered = recommendedMonthlyInvestment
                    .compareTo(goalRequiredMonthlyContribution) >= 0;

            if (Boolean.TRUE.equals(goalContributionCovered)) {
                reason.append("Your recommended monthly investment of ₹")
                        .append(recommendedMonthlyInvestment)
                        .append(" is sufficient to cover the required monthly contribution of ₹")
                        .append(goalRequiredMonthlyContribution)
                        .append(" for goal '").append(goalName).append("'.");
            } else {
                reason.append("Your recommended monthly investment of ₹")
                        .append(recommendedMonthlyInvestment)
                        .append(" is below the required monthly contribution of ₹")
                        .append(goalRequiredMonthlyContribution)
                        .append(" for goal '").append(goalName).append("'.");
                warnings.add("Recommended investment capacity (₹" + recommendedMonthlyInvestment
                        + ") does not fully cover the goal requirement (₹" + goalRequiredMonthlyContribution
                        + ") for '" + goalName + "'. Consider reducing expenses or extending the goal target date.");
            }
        } else {
            reason.append("General wealth accumulation recommendation based on positive disposable income of ₹")
                    .append(positiveDisposable)
                    .append(" and ").append(riskPreference).append(" risk profile.");
        }

        if (disposableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add("Disposable income is zero or negative. Investment recommendation is paused until spending is balanced.");
        }

        if (!emergencyFundReady) {
            warnings.add("Emergency fund is not fully funded (₹" + efRemaining
                    + " remaining). Prioritize emergency fund contributions; investment allocation is reduced to 15%.");
        }

        return new InvestmentRecommendationResponse(
                recommendedMonthlyInvestment,
                disposableIncome.setScale(SCALE, RoundingMode.HALF_UP),
                investmentAllocationPercentage.setScale(SCALE, RoundingMode.HALF_UP),
                emergencyFundReady,
                efRemaining.setScale(SCALE, RoundingMode.HALF_UP),
                riskPreference,
                equityPct.setScale(SCALE, RoundingMode.HALF_UP),
                debtPct.setScale(SCALE, RoundingMode.HALF_UP),
                goldPct.setScale(SCALE, RoundingMode.HALF_UP),
                equityAmount,
                debtAmount,
                goldAmount,
                diversified,
                diversificationStatus,
                diversificationMessage,
                goalName,
                goalTargetAmount,
                goalCurrentAmount,
                goalRemainingAmount,
                goalRequiredMonthlyContribution,
                goalContributionCovered,
                reason.toString(),
                warnings);
    }

    public static String assessDiversificationStatus(BigDecimal equityPct, BigDecimal debtPct, BigDecimal goldPct) {
        int activeAssets = 0;
        if (equityPct != null && equityPct.compareTo(BigDecimal.ZERO) > 0) activeAssets++;
        if (debtPct != null && debtPct.compareTo(BigDecimal.ZERO) > 0) activeAssets++;
        if (goldPct != null && goldPct.compareTo(BigDecimal.ZERO) > 0) activeAssets++;

        if (activeAssets >= 3) {
            return "DIVERSIFIED";
        } else if (activeAssets == 2) {
            return "PARTIALLY_DIVERSIFIED";
        } else {
            return "NOT_DIVERSIFIED";
        }
    }

    public static String buildDiversificationMessage(
            InvestmentRiskPreference riskPreference,
            BigDecimal equityPct,
            BigDecimal debtPct,
            BigDecimal goldPct,
            String status) {
        if (!"DIVERSIFIED".equals(status)) {
            return "Portfolio lacks multi-asset diversification. Spreading investments across equity, debt, and gold reduces concentration risk.";
        }
        return switch (riskPreference) {
            case CONSERVATIVE ->
                "Diversified allocation across Equity (" + equityPct + "%), Debt (" + debtPct + "%), and Gold (" + goldPct
                        + "%). Conservative profile emphasizes a debt-heavy allocation for lower volatility and capital preservation, with gold as an inflation hedge and equity providing modest growth.";
            case AGGRESSIVE ->
                "Diversified allocation across Equity (" + equityPct + "%), Debt (" + debtPct + "%), and Gold (" + goldPct
                        + "%). Aggressive profile emphasizes an equity-heavy allocation for higher long-term growth potential with higher risk tolerance, balanced by debt and gold reserves.";
            default ->
                "Diversified allocation across Equity (" + equityPct + "%), Debt (" + debtPct + "%), and Gold (" + goldPct
                        + "%). Moderate profile provides a balanced growth and stability strategy, combining equity for wealth generation, debt for steady returns, and gold as a hedge.";
        };
    }

    @Transactional(readOnly = true)
    public InvestmentProjectionResponse project(Long userId) {
        return project(userId, null, DEFAULT_PROJECTION_YEARS);
    }

    @Transactional(readOnly = true)
    public InvestmentProjectionResponse project(Long userId, Long goalId, Integer years) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        int projYears = (years != null) ? years : DEFAULT_PROJECTION_YEARS;
        if (projYears <= 0 || projYears > MAX_PROJECTION_YEARS) {
            throw new IllegalArgumentException("years must be between 1 and " + MAX_PROJECTION_YEARS);
        }

        InvestmentRecommendationResponse rec = recommend(userId, goalId);
        BigDecimal p = rec.recommendedMonthlyInvestment();
        InvestmentRiskPreference risk = rec.riskPreference();
        int months = projYears * 12;

        BigDecimal annualReturn = expectedReturnFor(risk);
        BigDecimal monthlyReturn = annualReturn.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

        BigDecimal fv = calculateFutureValue(p, annualReturn, months);
        BigDecimal totalContributed = (p.compareTo(BigDecimal.ZERO) > 0)
                ? p.multiply(BigDecimal.valueOf(months)).setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal estimatedGrowth = fv.subtract(totalContributed).max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);

        String goalName = rec.goalName();
        BigDecimal goalTargetAmount = rec.goalTargetAmount();
        BigDecimal goalCurrentAmount = rec.goalCurrentAmount();
        BigDecimal goalRemainingAmount = rec.goalRemainingAmount();
        Boolean goalCovered = null;
        if (goalRemainingAmount != null) {
            goalCovered = fv.compareTo(goalRemainingAmount) >= 0;
        }

        List<String> warnings = new ArrayList<>(rec.warnings());
        StringBuilder message = new StringBuilder();

        if (goalName != null) {
            if (Boolean.TRUE.equals(goalCovered)) {
                message.append("Projected future value of ₹").append(fv).append(" over ").append(projYears)
                        .append(" years (").append(months).append(" months) is projected to cover your remaining goal amount of ₹")
                        .append(goalRemainingAmount).append(" for '").append(goalName)
                        .append("'. Returns are illustrative (").append(annualReturn).append("% p.a. based on ")
                        .append(risk).append(" risk profile) and not guaranteed.");
            } else {
                message.append("Projected future value of ₹").append(fv).append(" over ").append(projYears)
                        .append(" years (").append(months).append(" months) is below your remaining goal amount of ₹")
                        .append(goalRemainingAmount).append(" for '").append(goalName)
                        .append("'. Consider extending the horizon or adjusting contributions. Returns are illustrative (")
                        .append(annualReturn).append("% p.a. based on ").append(risk).append(" risk profile) and not guaranteed.");
                warnings.add("Projected future value does not cover the remaining goal amount of ₹" + goalRemainingAmount + " for '" + goalName + "'.");
            }
        } else {
            if (p.compareTo(BigDecimal.ZERO) > 0) {
                message.append("Projected future value of ₹").append(fv).append(" over ").append(projYears)
                        .append(" years (").append(months).append(" months) based on a monthly SIP of ₹").append(p)
                        .append(". Total contributions: ₹").append(totalContributed).append(", estimated growth: ₹")
                        .append(estimatedGrowth).append(". Returns are illustrative (").append(annualReturn)
                        .append("% p.a. based on ").append(risk).append(" risk profile) and not guaranteed.");
            } else {
                message.append("Monthly investment is ₹0.00; projected future value is ₹0.00. Returns are illustrative and not guaranteed.");
            }
        }

        return new InvestmentProjectionResponse(
                p,
                projYears,
                months,
                annualReturn.setScale(SCALE, RoundingMode.HALF_UP),
                monthlyReturn,
                totalContributed,
                estimatedGrowth,
                fv,
                risk,
                goalName,
                goalTargetAmount,
                goalCurrentAmount,
                goalRemainingAmount,
                goalCovered,
                message.toString(),
                warnings);
    }

    public static BigDecimal expectedReturnFor(InvestmentRiskPreference riskPreference) {
        if (riskPreference == null) {
            return MODERATE_RETURN;
        }
        return switch (riskPreference) {
            case CONSERVATIVE -> CONSERVATIVE_RETURN;
            case AGGRESSIVE -> AGGRESSIVE_RETURN;
            default -> MODERATE_RETURN;
        };
    }

    public static BigDecimal calculateFutureValue(BigDecimal monthlyInvestment, BigDecimal annualReturnPercentage, int months) {
        if (monthlyInvestment == null || monthlyInvestment.compareTo(BigDecimal.ZERO) <= 0 || months <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        if (annualReturnPercentage == null || annualReturnPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return monthlyInvestment.multiply(BigDecimal.valueOf(months)).setScale(SCALE, RoundingMode.HALF_UP);
        }
        double p = monthlyInvestment.doubleValue();
        double r = annualReturnPercentage.doubleValue() / 1200.0;
        double factor = (Math.pow(1.0 + r, months) - 1.0) / r * (1.0 + r);
        double fv = p * factor;
        return BigDecimal.valueOf(fv).max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
