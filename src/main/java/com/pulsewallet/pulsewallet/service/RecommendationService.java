package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.RecommendationResponse;
import com.pulsewallet.pulsewallet.dto.RecommendationResponse.RecommendationItem;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.FraudAlertRepository;

@Service
public class RecommendationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;

    private final FinancialSummaryService financialSummaryService;
    private final BudgetService budgetService;
    private final FraudAlertRepository fraudAlertRepository;
    private final FinancialGoalService financialGoalService;
    private final Clock clock;
    private final FinancialPlanService financialPlanService;
    private final CreditCardIntelligenceService creditCardIntelligenceService;

    public RecommendationService(
            FinancialSummaryService financialSummaryService,
            BudgetService budgetService,
            FraudAlertRepository fraudAlertRepository,
            FinancialGoalService financialGoalService,
            Clock clock) {
        this.financialSummaryService = financialSummaryService;
        this.budgetService = budgetService;
        this.fraudAlertRepository = fraudAlertRepository;
        this.financialGoalService = financialGoalService;
        this.clock = clock;
        this.financialPlanService = null;
        this.creditCardIntelligenceService = null;
    }

    @Autowired
    public RecommendationService(
            FinancialSummaryService financialSummaryService,
            BudgetService budgetService,
            FraudAlertRepository fraudAlertRepository,
            FinancialGoalService financialGoalService,
            Clock clock,
            FinancialPlanService financialPlanService,
            CreditCardIntelligenceService creditCardIntelligenceService) {
        this.financialSummaryService = financialSummaryService;
        this.budgetService = budgetService;
        this.fraudAlertRepository = fraudAlertRepository;
        this.financialGoalService = financialGoalService;
        this.clock = clock;
        this.financialPlanService = financialPlanService;
        this.creditCardIntelligenceService = creditCardIntelligenceService;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (financialPlanService != null) {
            return recommendFromPlan(userId);
        }

        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, null, null);
        List<BudgetResponse> budgets = budgetService.list(userId);
        BigDecimal monthlyIncome = monthlyAverage(summary, TransactionType.INCOME);
        BigDecimal monthlyExpenses = monthlyAverage(summary, TransactionType.EXPENSE);
        BigDecimal monthlyLeft = monthlyIncome.subtract(monthlyExpenses).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        List<RecommendationItem> recommendations = new ArrayList<>();
        List<FinancialGoalResponse> goals = financialGoalService.list(userId);
        BigDecimal requiredGoalSavings = ZERO;
        for (FinancialGoalResponse goal : goals) {
            if (!"COMPLETED".equals(goal.status())) {
                requiredGoalSavings = requiredGoalSavings.add(value(goal.requiredMonthlySaving()));
            }
        }
        requiredGoalSavings = requiredGoalSavings.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        addGoalRecommendations(goals, monthlyLeft, recommendations);

        addBudgetRecommendations(summary, budgets, recommendations);
        long unresolvedAlerts = fraudAlertRepository.countByUserIdAndResolvedFalse(userId);
        if (unresolvedAlerts > 0) {
            recommendations.add(item("WARNING", "Review unresolved fraud alerts",
                    unresolvedAlerts + " unresolved risk alert(s) should be reviewed before moving money.", "HIGH",
                    null, "Unresolved alerts can affect available funds and transaction safety.",
                    "The fraud alert repository reports " + unresolvedAlerts + " unresolved alert(s) for this user."));
        }

        BigDecimal goalSavings = monthlyLeft.max(ZERO).min(requiredGoalSavings);
        BigDecimal investments = ZERO;
        BigDecimal buffer = ZERO;
        BigDecimal flexibleSpending = ZERO;
        if (monthlyLeft.compareTo(ZERO) > 0) {
            BigDecimal afterGoals = monthlyLeft.subtract(goalSavings).max(ZERO);
            buffer = requiredGoalSavings.signum() > 0 ? afterGoals.min(monthlyExpenses)
                    : monthlyExpenses.min(monthlyLeft);
            flexibleSpending = afterGoals.subtract(buffer).max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (requiredGoalSavings.signum() == 0) {
                recommendations.add(item("BUFFER_RECOMMENDATION", "Build a financial buffer",
                        "You have " + money(monthlyLeft) + " left on average this month. Consider keeping "
                                + money(buffer) + " available as a buffer before investing or discretionary spending.",
                        "MEDIUM", buffer,
                        "A buffer up to one observed month of expenses protects your current cash position.",
                        "The amount is based on your observed average monthly expenses of " + money(monthlyExpenses)
                                + "."));
            } else {
                recommendations.add(item("SAVING_RECOMMENDATION", "Fund your active goals first",
                        "Your active goals require " + money(requiredGoalSavings) + "/month. "
                                + (requiredGoalSavings.compareTo(monthlyLeft) <= 0
                                        ? "Your current monthly surplus can cover that requirement."
                                        : "Your current monthly surplus cannot cover that requirement."),
                        requiredGoalSavings.compareTo(monthlyLeft) <= 0 ? "MEDIUM" : "HIGH", goalSavings,
                        "Goal contributions are prioritized from the available monthly surplus.",
                        "Required monthly saving is calculated from each active goal's remaining amount and deadline."));
            }
            recommendations.add(item("INVESTMENT_RECOMMENDATION", "No investment plan is recorded",
                    "After the suggested buffer, " + money(flexibleSpending)
                            + " remains flexible. Add an investment plan before assigning an investment amount.",
                    "LOW", ZERO, "No investment allocation is available in the current financial data.",
                    "No investment account or allocation data exists in the current schema."));
        } else if (monthlyLeft.compareTo(ZERO) < 0) {
            recommendations.add(item("WARNING", "Monthly spending exceeds income",
                    "Your average monthly expenses exceed income by " + money(monthlyLeft.abs())
                            + ". Reduce planned spending before adding savings or investments.",
                    "HIGH", monthlyLeft.abs(), "Negative monthly left reduces financial resilience.",
                    "Monthly left is calculated as monthly income minus monthly expenses."));
        } else {
            recommendations.add(item("POSITIVE_FEEDBACK", "No monthly surplus is available",
                    "Income and expenses currently balance. Track another month before assigning savings or investment amounts.",
                    "LOW", ZERO, "There is no unallocated monthly surplus in the available transaction history.",
                    "Monthly left is zero based on the available income and expense transactions."));
        }

        if (recommendations.isEmpty()) {
            recommendations.add(item("POSITIVE_FEEDBACK", "Your spending data is within current budgets",
                    "No budget overruns or unresolved risk alerts were found in the available data.", "LOW", ZERO,
                    "Current data does not indicate an immediate budget issue.",
                    "Budgets and fraud alerts were evaluated for the authenticated user."));
        }
        recommendations
                .sort((left, right) -> Integer.compare(priorityRank(left.priority()), priorityRank(right.priority())));

        return new RecommendationResponse(
                money(monthlyIncome), money(monthlyExpenses), money(monthlyLeft), money(goalSavings.add(buffer)),
                money(investments), money(requiredGoalSavings), money(buffer), money(flexibleSpending), recommendations,
                "deterministic-rules-v1");
    }

    private RecommendationResponse recommendFromPlan(Long userId) {
        var plan = financialPlanService.calculate(userId);
        List<RecommendationItem> recommendations = new ArrayList<>();
        List<FinancialGoalResponse> goals = financialGoalService.list(userId);
        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, null, null);
        addGoalRecommendations(goals, plan.monthlyLeft(), recommendations);
        addBudgetRecommendations(summary, budgetService.list(userId), recommendations);

        for (String warning : plan.warnings()) {
            recommendations.add(item("WARNING", "Review your financial plan", warning, "MEDIUM", null,
                    "The plan is constrained by observed income, expenses, goals, and your profile.",
                    "The financial plan calculation produced this warning."));
        }
        if (plan.recommendedInvestmentAmount().signum() > 0) {
            recommendations.add(item("INVESTMENT_RECOMMENDATION", "You have investment capacity",
                    money(plan.recommendedInvestmentAmount())
                            + " is available after observed expenses, active goal savings, and the recommended buffer.",
                    "MEDIUM", plan.recommendedInvestmentAmount(),
                    "Educational allocation only; review your circumstances before acting.",
                    "The amount is the nonnegative surplus remaining after prioritized allocations."));
        }
        addCreditCardRecommendations(userId, recommendations);
        if (recommendations.isEmpty()) {
            recommendations.add(item("POSITIVE_FEEDBACK", "Your plan is ready",
                    "No additional warnings were found in the available financial data.", "LOW", ZERO,
                    "The plan is based on authenticated user data.",
                    "Current transactions, goals, and profile were evaluated."));
        }
        recommendations
                .sort((left, right) -> Integer.compare(priorityRank(left.priority()), priorityRank(right.priority())));
        return new RecommendationResponse(plan.monthlyIncome(), plan.monthlyExpenses(), plan.monthlyLeft(),
                plan.goalSavingsRequired().add(plan.recommendedEmergencyAllocation()).setScale(MONEY_SCALE,
                        RoundingMode.HALF_UP),
                plan.recommendedInvestmentAmount(), plan.goalSavingsRequired(), plan.recommendedEmergencyAllocation(),
                plan.flexibleSpendingAmount(), recommendations, "deterministic-rules-v2");
    }

    private void addGoalRecommendations(List<FinancialGoalResponse> goals, BigDecimal monthlyLeft,
            List<RecommendationItem> recommendations) {
        for (FinancialGoalResponse goal : goals) {
            if ("COMPLETED".equals(goal.status())) {
                recommendations.add(item("POSITIVE_FEEDBACK", goal.name() + " is complete",
                        "Congratulations! Your " + goal.name() + " goal is complete.", "LOW", goal.currentAmount(),
                        "The target amount has been fully saved.", "Current saved amount meets the goal target."));
            } else if ("OVERDUE".equals(goal.status())) {
                recommendations.add(item("WARNING", goal.name() + " is overdue",
                        "Your " + goal.name() + " goal is past its target date with " + money(goal.remainingAmount())
                                + " remaining.",
                        "HIGH", goal.remainingAmount(),
                        "The target date has passed before the goal was completed.",
                        "Goal date and current saved amount were evaluated."));
            } else if ("NEEDS_ATTENTION".equals(goal.status())) {
                recommendations.add(item("GOAL_RECOMMENDATION", goal.name() + " needs attention",
                        "Your " + goal.name() + " goal needs approximately " + money(goal.requiredMonthlySaving())
                                + "/month, but your current monthly left is " + money(monthlyLeft) + ".",
                        "HIGH", goal.requiredMonthlySaving(),
                        "Consider extending the deadline or reducing discretionary spending.",
                        "Required monthly saving exceeds the current monthly left."));
            }
        }
    }

    private int priorityRank(String priority) {
        return switch (priority) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }

    private void addBudgetRecommendations(
            FinancialSummaryResponse summary,
            List<BudgetResponse> budgets,
            List<RecommendationItem> recommendations) {
        Map<Long, BigDecimal> currentCategorySpend = new HashMap<>();
        LocalDate latestDate = null;
        for (FinancialSummaryResponse.CategoryMonthlyAmount row : summary.categoryMonthlyTotals()) {
            LocalDate date = LocalDate.of(row.year(), row.month(), 1);
            if (latestDate == null || date.isAfter(latestDate)) {
                latestDate = date;
            }
        }
        if (latestDate == null) {
            return;
        }
        for (FinancialSummaryResponse.CategoryMonthlyAmount row : summary.categoryMonthlyTotals()) {
            if (row.year() == latestDate.getYear() && row.month() == latestDate.getMonthValue()
                    && row.categoryId() != null) {
                BigDecimal previous = currentCategorySpend.getOrDefault(row.categoryId(), ZERO);
                currentCategorySpend.put(row.categoryId(), previous.add(value(row.total())));
            }
        }
        LocalDate today = LocalDate.now(clock);
        for (BudgetResponse budget : budgets) {
            if (today.isBefore(budget.startDate()) || today.isAfter(budget.endDate())) {
                continue;
            }
            BigDecimal spent = budget.categoryId() == null ? ZERO
                    : currentCategorySpend.getOrDefault(budget.categoryId(), ZERO);
            if (budget.categoryId() != null && spent.compareTo(value(budget.amount())) > 0) {
                BigDecimal over = spent.subtract(value(budget.amount())).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                recommendations.add(item("SPENDING_RECOMMENDATION", "Budget exceeded in " + budget.categoryName(),
                        "Your latest recorded monthly spending is " + money(over) + " above the "
                                + budget.categoryName() + " budget.",
                        "HIGH", over,
                        "Reduce " + budget.categoryName() + " spending to return within its existing budget.",
                        "Latest recorded category spending was compared with the user's budget."));
            }
        }
    }

    private BigDecimal monthlyAverage(FinancialSummaryResponse summary, TransactionType type) {
        Set<String> months = new HashSet<>();
        BigDecimal total = ZERO;
        for (FinancialSummaryResponse.MonthlyTypeAmount row : summary.monthlyIncomeExpense()) {
            months.add(row.year() + "-" + row.month());
            if (row.type() == type) {
                total = total.add(value(row.total()));
            }
        }
        return months.isEmpty() ? ZERO
                : total.divide(BigDecimal.valueOf(months.size()), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private RecommendationItem item(String type, String title, String message, String priority,
            BigDecimal relatedAmount, String impact, String reason) {
        return new RecommendationItem(type, title, message, priority,
                relatedAmount == null ? null : money(relatedAmount),
                null, impact, reason);
    }

    private void addCreditCardRecommendations(Long userId, List<RecommendationItem> recommendations) {
        if (creditCardIntelligenceService == null) return;
        try {
            CreditCardIntelligenceResponse intel = creditCardIntelligenceService.getIntelligence(userId);
            if (intel.cards().isEmpty()) return;

            var summary = intel.summary();
            var repayment = intel.repaymentCapacity();

            if (summary.overallUtilization().compareTo(new BigDecimal("75")) > 0) {
                recommendations.add(item("CREDIT_CARD_WARNING", "High overall credit utilization",
                        "Your overall credit card utilization is " + summary.overallUtilization()
                                + "%. Consider reducing outstanding balances to improve financial flexibility.",
                        "HIGH", summary.totalOutstanding(),
                        "High utilization can reduce your available credit buffer.",
                        "Overall utilization is calculated as total outstanding / total credit limit (PulseWallet internal threshold)."));
            } else if (summary.overallUtilization().compareTo(new BigDecimal("50")) > 0) {
                recommendations.add(item("CREDIT_CARD_WARNING", "Moderate credit utilization",
                        "Your overall credit card utilization is " + summary.overallUtilization()
                                + "%. Monitor your spending to prevent it from increasing further.",
                        "MEDIUM", summary.totalOutstanding(),
                        "Moderate utilization warrants monitoring.",
                        "Overall utilization is calculated as total outstanding / total credit limit (PulseWallet internal threshold)."));
            }

            if ("HIGH_RISK".equals(repayment.status()) || "STRETCHED".equals(repayment.status())) {
                recommendations.add(item("CREDIT_CARD_WARNING", "Repayment capacity is " + repayment.status().toLowerCase().replace('_', ' '),
                        "Your total card outstanding of " + money(summary.totalOutstanding())
                                + " may be difficult to repay from your current available surplus of "
                                + money(repayment.availableAfterPlan()) + ".",
                        "HIGH", summary.totalOutstanding(),
                        "Consider a repayment plan or reducing discretionary card spending.",
                        "PulseWallet Repayment Capacity assessment based on financial plan allocations."));
            }

            if (summary.cardsNeedingAttention() > 0) {
                recommendations.add(item("CREDIT_CARD_WARNING", summary.cardsNeedingAttention() + " card(s) need attention",
                        summary.cardsNeedingAttention() + " of your " + summary.numberOfCards()
                                + " card(s) have high utilization. Review individual card health for details.",
                        "MEDIUM", null,
                        "Cards with high utilization reduce your financial flexibility.",
                        "Cards with utilization above 50% are flagged (PulseWallet internal threshold)."));
            }

            if (summary.cardsWithUpcomingDueDate() > 0) {
                recommendations.add(item("CREDIT_CARD_REMINDER", "Upcoming payment due",
                        summary.cardsWithUpcomingDueDate() + " card(s) have payment due dates within the next 7 days.",
                        "HIGH", null,
                        "Timely payments avoid late fees and maintain card health.",
                        "Due dates are compared against the current date."));
            }

            if (summary.overallUtilization().compareTo(new BigDecimal("30")) <= 0
                    && summary.totalOutstanding().signum() >= 0
                    && "COMFORTABLE".equals(repayment.status())) {
                recommendations.add(item("POSITIVE_FEEDBACK", "Healthy credit card usage",
                        "Your credit card utilization is low and repayment capacity is comfortable. Keep it up!",
                        "LOW", null,
                        "Low utilization and comfortable repayment indicate good credit card management.",
                        "PulseWallet Credit Card Intelligence assessment."));
            }
        } catch (Exception e) {
            // Credit card intelligence is optional — don't break recommendations if it fails
        }
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }

    private BigDecimal money(BigDecimal amount) {
        return value(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}