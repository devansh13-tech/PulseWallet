package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.AdvisoryResponse;
import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.GoalAchievementResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.dto.MonthlyAdviceResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.MonthlyTypeTotalProjection;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@Service
public class AdvisoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal EMERGENCY_FUND_MONTHS = new BigDecimal("6");
    private static final BigDecimal MEDICAL_RESERVE_MONTHS = new BigDecimal("3");
    private static final BigDecimal SAVINGS_PERCENT = new BigDecimal("0.30");
    private static final BigDecimal INVESTMENT_PERCENT = new BigDecimal("0.15");
    private static final int MONEY_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final Clock clock;
    private final FinancialSummaryService financialSummaryService;
    private final EmergencyFundService emergencyFundService;
    private final FinancialGoalService financialGoalService;
    private final FinancialGoalRepository financialGoalRepository;
    private final InvestmentRecommendationService investmentRecommendationService;

    @Autowired
    public AdvisoryService(
            TransactionRepository transactionRepository,
            Clock clock,
            FinancialSummaryService financialSummaryService,
            EmergencyFundService emergencyFundService,
            FinancialGoalService financialGoalService,
            FinancialGoalRepository financialGoalRepository,
            InvestmentRecommendationService investmentRecommendationService) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
        this.financialSummaryService = financialSummaryService;
        this.emergencyFundService = emergencyFundService;
        this.financialGoalService = financialGoalService;
        this.financialGoalRepository = financialGoalRepository;
        this.investmentRecommendationService = investmentRecommendationService;
    }

    public AdvisoryService(TransactionRepository transactionRepository, Clock clock) {
        this(transactionRepository, clock, null, null, null, null, null);
    }

    public AdvisoryService(TransactionRepository transactionRepository) {
        this(transactionRepository, Clock.systemDefaultZone());
    }

    @Transactional(readOnly = true)
    public AdvisoryResponse advice(Long userId) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusMonths(12).withDayOfMonth(1);

        List<MonthlyTypeTotalProjection> rows = transactionRepository.sumByMonthAndType(userId, from, to);
        BigDecimal monthlyIncome = ZERO;
        BigDecimal monthlyExpenses = ZERO;

        for (MonthlyTypeTotalProjection row : rows) {
            BigDecimal total = row.getTotal() != null ? row.getTotal() : ZERO;
            if (row.getType() == TransactionType.INCOME) {
                monthlyIncome = monthlyIncome.add(total);
            } else if (row.getType() == TransactionType.EXPENSE) {
                monthlyExpenses = monthlyExpenses.add(total);
            }
        }

        BigDecimal disposableIncome = monthlyIncome.subtract(monthlyExpenses);
        if (rows.isEmpty()) {
            disposableIncome = ZERO;
        }
        BigDecimal emergencyFundTarget = monthlyExpenses.multiply(EMERGENCY_FUND_MONTHS).setScale(MONEY_SCALE,
                RoundingMode.HALF_UP);
        BigDecimal shortTermReserveTarget = monthlyExpenses.multiply(MEDICAL_RESERVE_MONTHS).setScale(MONEY_SCALE,
                RoundingMode.HALF_UP);

        BigDecimal recommendedSavings = disposableIncome.compareTo(ZERO) > 0
                ? disposableIncome.multiply(SAVINGS_PERCENT).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;
        BigDecimal recommendedInvestment = disposableIncome.compareTo(ZERO) > 0
                ? disposableIncome.multiply(INVESTMENT_PERCENT).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;

        String guidance = "illustrative allocation only; not personalised financial advice and not a regulated advisory claim.";

        return new AdvisoryResponse(
                monthlyIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                monthlyExpenses.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                disposableIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                emergencyFundTarget,
                shortTermReserveTarget,
                recommendedSavings,
                recommendedInvestment,
                guidance);
    }

    @Transactional(readOnly = true)
    public MonthlyAdviceResponse monthlyAdvice(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        // 1. Income, expenses, disposable income, savings rate from FinancialSummaryService
        FinancialSummaryResponse summary = financialSummaryService != null
                ? financialSummaryService.summarize(userId, null, null)
                : null;

        BigDecimal totalIncome = (summary != null && summary.totalIncome() != null)
                ? summary.totalIncome().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalExpenses = (summary != null && summary.totalExpenses() != null)
                ? summary.totalExpenses().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal disposableIncome = (summary != null && summary.disposableIncome() != null)
                ? summary.disposableIncome().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal savingsRate = (summary != null && summary.savingsRate() != null)
                ? summary.savingsRate().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // 2. Emergency fund status from EmergencyFundService
        EmergencyFundPlanResponse efPlan = null;
        if (emergencyFundService != null) {
            try {
                efPlan = emergencyFundService.calculate(userId);
            } catch (Exception ignored) {
            }
        }

        BigDecimal emergencyFundRemaining = (efPlan != null && efPlan.remainingAmountNeeded() != null)
                ? efPlan.remainingAmountNeeded().max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        boolean emergencyFundReady = efPlan != null && emergencyFundRemaining.compareTo(ZERO) <= 0;

        // 3. Investment recommendation from InvestmentRecommendationService
        InvestmentRecommendationResponse rec = null;
        if (investmentRecommendationService != null) {
            try {
                rec = investmentRecommendationService.recommend(userId);
            } catch (Exception ignored) {
            }
        }

        BigDecimal recommendedMonthlyInvestment = (rec != null && rec.recommendedMonthlyInvestment() != null)
                ? rec.recommendedMonthlyInvestment().max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        InvestmentRiskPreference riskPreference = (rec != null && rec.riskPreference() != null)
                ? rec.riskPreference()
                : InvestmentRiskPreference.MODERATE;
        boolean diversified = rec != null && rec.diversified();
        String diversificationStatus = (rec != null && rec.diversificationStatus() != null)
                ? rec.diversificationStatus()
                : "NOT_DIVERSIFIED";

        // 4. Primary active goal analysis from FinancialGoalService
        String primaryGoalName = null;
        String primaryGoalStatus = null;
        BigDecimal primaryGoalShortfall = null;

        if (financialGoalRepository != null && financialGoalService != null) {
            List<FinancialGoal> userGoals = financialGoalRepository.findByUserIdOrderByTargetDateAsc(userId);
            for (FinancialGoal g : userGoals) {
                if (g.getCurrentAmount() == null || g.getTargetAmount() == null
                        || g.getCurrentAmount().compareTo(g.getTargetAmount()) < 0) {
                    try {
                        GoalAchievementResponse achievement = financialGoalService.getAchievement(g.getId(), userId);
                        if (achievement != null) {
                            primaryGoalName = achievement.goalName();
                            primaryGoalStatus = achievement.achievementStatus();
                            primaryGoalShortfall = achievement.monthlyShortfall() != null
                                    ? achievement.monthlyShortfall().max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                                    : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return calculateMonthlyAdvice(
                totalIncome,
                totalExpenses,
                disposableIncome,
                savingsRate,
                emergencyFundReady,
                emergencyFundRemaining,
                recommendedMonthlyInvestment,
                riskPreference,
                diversified,
                diversificationStatus,
                primaryGoalName,
                primaryGoalStatus,
                primaryGoalShortfall);
    }

    public MonthlyAdviceResponse calculateMonthlyAdvice(
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal disposableIncome,
            BigDecimal savingsRate,
            boolean emergencyFundReady,
            BigDecimal emergencyFundRemaining,
            BigDecimal recommendedMonthlyInvestment,
            InvestmentRiskPreference riskPreference,
            boolean diversified,
            String diversificationStatus,
            String primaryGoalName,
            String primaryGoalStatus,
            BigDecimal primaryGoalShortfall) {
        BigDecimal income = totalIncome != null ? totalIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal expenses = totalExpenses != null ? totalExpenses.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal disposable = disposableIncome != null ? disposableIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal rate = savingsRate != null ? savingsRate.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal efRemaining = emergencyFundRemaining != null ? emergencyFundRemaining.max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal recInvestment = recommendedMonthlyInvestment != null ? recommendedMonthlyInvestment.max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP) : ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        InvestmentRiskPreference risk = riskPreference != null ? riskPreference : InvestmentRiskPreference.MODERATE;
        String divStatus = diversificationStatus != null ? diversificationStatus : "NOT_DIVERSIFIED";
        BigDecimal goalShortfall = primaryGoalShortfall != null ? primaryGoalShortfall.max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP) : null;

        List<String> recommendations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String advicePriority;
        String advice;

        boolean noTransactions = income.compareTo(ZERO) == 0 && expenses.compareTo(ZERO) == 0;

        if (noTransactions) {
            advicePriority = "PRIORITY_2";
            advice = "No financial activity recorded yet. Start by logging your income and regular expenses to establish a budget and receive personalized advice.";
            recommendations.add("Log your income and regular expenses to analyze your cash flow.");
            recommendations.add("Create an emergency fund plan once your typical monthly expenses are established.");
            warnings.add("No transactions found for financial analysis.");
        } else if (!emergencyFundReady && efRemaining.compareTo(ZERO) > 0) {
            advicePriority = "PRIORITY_1";
            advice = "Your emergency fund is not fully funded. Prioritize building the remaining emergency reserve before increasing investment contributions.";
            recommendations.add("Prioritize building the remaining emergency reserve (₹" + efRemaining + " remaining) before increasing investments.");
            if (recInvestment.compareTo(ZERO) > 0) {
                recommendations.add("Investment allocation is prudently reduced to 15% (₹" + recInvestment + "/month) to balance emergency fund building with long-term growth.");
            }
            if (disposable.compareTo(ZERO) <= 0) {
                recommendations.add("Focus on reducing discretionary spending to free up funds for your emergency reserve.");
                warnings.add("Disposable income is zero or negative. Investment recommendation is paused until spending is balanced.");
            }
            warnings.add("Emergency fund is not fully funded (₹" + efRemaining + " remaining).");
        } else if (disposable.compareTo(ZERO) <= 0) {
            advicePriority = "PRIORITY_2";
            if (disposable.compareTo(ZERO) < 0) {
                advice = "Your expenses currently exceed your income. Focus on reducing discretionary spending before investing.";
                recommendations.add("Reduce discretionary spending immediately to eliminate the monthly deficit of ₹" + disposable.abs() + ".");
            } else {
                advice = "Your expenses currently consume all of your income. Focus on reducing discretionary spending to create positive cash flow.";
                recommendations.add("Audit regular expenses and eliminate non-essential spending to build monthly savings capacity.");
            }
            recommendations.add("Investment recommendations are paused until you have positive disposable income.");
            warnings.add("Disposable income is zero or negative. Investment recommendation is paused until spending is balanced.");
        } else if (primaryGoalStatus != null && ("AT_RISK".equals(primaryGoalStatus) || "OVERDUE".equals(primaryGoalStatus) || (goalShortfall != null && goalShortfall.compareTo(ZERO) > 0))) {
            advicePriority = "PRIORITY_3";
            advice = "Your current investment capacity is below the monthly contribution required for your goal. Consider increasing savings or extending the goal timeline.";
            if (goalShortfall != null && goalShortfall.compareTo(ZERO) > 0) {
                recommendations.add("Increase monthly allocation by ₹" + goalShortfall + " to stay on track for '" + primaryGoalName + "'.");
                recommendations.add("Consider extending the target date for '" + primaryGoalName + "' or reducing expenses to close the monthly shortfall.");
                warnings.add("Monthly shortfall of ₹" + goalShortfall + " for goal '" + primaryGoalName + "'.");
            }
            if ("OVERDUE".equals(primaryGoalStatus)) {
                recommendations.add("Goal '" + primaryGoalName + "' has passed its target date. Review and update the target date to resume planning.");
                warnings.add("Goal '" + primaryGoalName + "' is overdue.");
            }
        } else if (!diversified) {
            advicePriority = "PRIORITY_5";
            advice = "Your asset allocation is not fully diversified. Distribute your investments across Equity, Debt, and Gold to reduce concentration risk.";
            recommendations.add("Distribute your monthly investment of ₹" + recInvestment + " across multi-asset classes for balanced growth and protection.");
            if (primaryGoalName != null) {
                recommendations.add("Goal '" + primaryGoalName + "' is on track. Keep up the current contribution pace.");
            }
        } else if (rate.compareTo(new BigDecimal("20.00")) < 0) {
            advicePriority = "PRIORITY_4";
            advice = "Your emergency fund is ready. Recommended monthly investment is ₹" + recInvestment + " to grow your wealth over time and increase your savings rate.";
            recommendations.add("Deploy ₹" + recInvestment + "/month toward your " + risk + " investment portfolio.");
            recommendations.add("Aim to increase your savings rate from " + rate + "% toward the recommended 20%+ target.");
            if (primaryGoalName != null) {
                recommendations.add("Goal '" + primaryGoalName + "' is on track. Keep up the current contribution pace.");
            }
        } else {
            advicePriority = "PRIORITY_6";
            advice = "Your cash flow is positive and your emergency fund is ready. Continue your monthly investment plan and maintain diversification.";
            recommendations.add("Continue your recommended monthly investment of ₹" + recInvestment + " (" + risk + " profile).");
            recommendations.add("Maintain your diversified asset allocation to balance growth and capital preservation.");
            if (primaryGoalName != null) {
                recommendations.add("Goal '" + primaryGoalName + "' is on track. Keep up the current contribution pace.");
            }
        }

        return new MonthlyAdviceResponse(
                income,
                expenses,
                disposable,
                rate,
                emergencyFundReady,
                efRemaining,
                recInvestment,
                risk,
                diversified,
                divStatus,
                primaryGoalName,
                primaryGoalStatus,
                goalShortfall,
                advicePriority,
                advice,
                recommendations,
                warnings);
    }
}
