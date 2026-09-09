package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.FinancialPlanResponse;
import com.pulsewallet.pulsewallet.dto.FinancialPlanResponse.Allocation;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;

@Service
public class FinancialPlanService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final int SCALE = 2;

    private final FinancialSummaryService summaryService;
    private final FinancialGoalService goalService;
    private final InvestmentProfileRepository profileRepository;

    public FinancialPlanService(
            FinancialSummaryService summaryService,
            FinancialGoalService goalService,
            InvestmentProfileRepository profileRepository) {
        this.summaryService = summaryService;
        this.goalService = goalService;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public FinancialPlanResponse calculate(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("userId must not be null");

        FinancialSummaryResponse summary = summaryService.summarize(userId, null, null);
        BigDecimal monthlyIncome = monthlyAverage(summary, TransactionType.INCOME);
        BigDecimal monthlyExpenses = monthlyAverage(summary, TransactionType.EXPENSE);
        BigDecimal monthlyLeft = scale(monthlyIncome.subtract(monthlyExpenses));
        List<FinancialGoalResponse> goals = goalService.list(userId);
        BigDecimal requiredGoals = scale(goals.stream()
                .filter(goal -> !"COMPLETED".equals(goal.status()))
                .map(goal -> value(goal.requiredMonthlySaving()))
                .reduce(BigDecimal.ZERO, (total, saving) -> total.add(saving)));

        BigDecimal available = monthlyLeft.max(BigDecimal.ZERO);
        BigDecimal goalAllocation = available.min(requiredGoals);
        BigDecimal afterGoals = available.subtract(goalAllocation).max(BigDecimal.ZERO);
        BigDecimal reserveTarget = scale(monthlyExpenses.multiply(THREE));
        BigDecimal reserveAllocation = scale(
                afterGoals.min(reserveTarget.divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP)));
        BigDecimal investmentAmount = scale(afterGoals.subtract(reserveAllocation).max(BigDecimal.ZERO));
        InvestmentProfile profile = profileRepository.findByUserId(userId).orElse(null);
        BigDecimal flexible = profile == null ? scale(afterGoals.subtract(reserveAllocation).max(BigDecimal.ZERO))
                : ZERO;
        List<String> warnings = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        if (monthlyLeft.signum() < 0) {
            warnings.add(
                    "Your expenses exceed observed monthly income, so no emergency or investment allocation is recommended.");
        }
        if (requiredGoals.compareTo(available) > 0) {
            warnings.add(
                    "Active goals require more than your observed monthly surplus; investment allocation is limited until the requirement is manageable.");
        }
        if (monthlyIncome.signum() > 0 && monthlyExpenses.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                .compareTo(new BigDecimal("0.8")) >= 0) {
            warnings.add(
                    "Observed expenses consume most of monthly income. Consider reducing discretionary spending before increasing investments.");
        }
        if (reserveTarget.signum() > 0 && reserveAllocation
                .compareTo(reserveTarget.divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP)) < 0) {
            warnings.add(
                    "The available surplus does not fully support the recommended monthly emergency/buffer allocation.");
        }
        if (profile == null) {
            warnings.add(
                    "Investment profile is not configured; investment allocation is unavailable until risk preference and time horizon are provided.");
        }

        reasons.add(
                "Monthly left is calculated from observed average monthly income minus observed average monthly expenses.");
        reasons.add("Goal savings includes active goals only; completed goals are excluded.");
        reasons.add(
                "Emergency reserve target is three observed months of expenses. No existing savings balance is assumed.");
        if (profile != null) {
            reasons.add(
                    "Investment percentages are educational and illustrative, based on the selected risk preference and time horizon.");
        }

        List<Allocation> allocation = allocations(profile, investmentAmount);
        return new FinancialPlanResponse(monthlyIncome, monthlyExpenses, monthlyLeft, requiredGoals, reserveTarget,
                reserveAllocation, profile == null ? ZERO : investmentAmount, flexible,
                profile == null ? null : profile.getRiskPreference(),
                profile == null ? null : profile.getTimeHorizonYears(), allocation, warnings, reasons);
    }

    private List<Allocation> allocations(InvestmentProfile profile, BigDecimal amount) {
        if (profile == null || amount.signum() == 0) {
            return List.of(new Allocation("Equity-oriented", ZERO, ZERO),
                    new Allocation("Debt/fixed-income-oriented", ZERO, ZERO),
                    new Allocation("Cash/liquid/emergency reserve", ZERO, ZERO));
        }
        int equity;
        int cash;
        switch (profile.getRiskPreference()) {
            case CONSERVATIVE -> {
                equity = 20;
                cash = 10;
            }
            case AGGRESSIVE -> {
                equity = 75;
                cash = 5;
            }
            default -> {
                equity = 50;
                cash = 10;
            }
        }
        if (profile.getTimeHorizonYears() <= 2)
            equity = Math.min(equity, 20);
        int debt = 100 - equity - cash;
        BigDecimal equityAmount = amount.multiply(BigDecimal.valueOf(equity)).movePointLeft(2).setScale(SCALE,
                RoundingMode.HALF_UP);
        BigDecimal cashAmount = amount.multiply(BigDecimal.valueOf(cash)).movePointLeft(2).setScale(SCALE,
                RoundingMode.HALF_UP);
        BigDecimal debtAmount = amount.subtract(equityAmount).subtract(cashAmount).setScale(SCALE,
                RoundingMode.HALF_UP);
        return List.of(new Allocation("Equity-oriented", BigDecimal.valueOf(equity), equityAmount),
                new Allocation("Debt/fixed-income-oriented", BigDecimal.valueOf(debt), debtAmount),
                new Allocation("Cash/liquid/emergency reserve", BigDecimal.valueOf(cash), cashAmount));
    }

    private BigDecimal monthlyAverage(FinancialSummaryResponse summary, TransactionType type) {
        java.util.Set<String> months = new java.util.HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (FinancialSummaryResponse.MonthlyTypeAmount row : summary.monthlyIncomeExpense()) {
            months.add(row.year() + "-" + row.month());
            if (row.type() == type)
                total = total.add(value(row.total()));
        }
        return months.isEmpty() ? ZERO
                : scale(total.divide(BigDecimal.valueOf(months.size()), SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal scale(BigDecimal amount) {
        return value(amount).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
