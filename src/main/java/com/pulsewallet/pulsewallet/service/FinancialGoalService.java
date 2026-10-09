package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.FinancialGoalRequest;
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.GoalAchievementResponse;
import com.pulsewallet.pulsewallet.dto.GoalPlanResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository financialGoalRepository;
    private final UserRepository userRepository;
    private final FinancialSummaryService financialSummaryService;
    private final Clock clock;
    private InvestmentRecommendationService investmentRecommendationService;

    @Autowired
    public FinancialGoalService(
            FinancialGoalRepository financialGoalRepository,
            UserRepository userRepository,
            FinancialSummaryService financialSummaryService,
            Clock clock,
            @Lazy InvestmentRecommendationService investmentRecommendationService) {
        this.financialGoalRepository = financialGoalRepository;
        this.userRepository = userRepository;
        this.financialSummaryService = financialSummaryService;
        this.clock = clock;
        this.investmentRecommendationService = investmentRecommendationService;
    }

    public FinancialGoalService(
            FinancialGoalRepository financialGoalRepository,
            UserRepository userRepository,
            FinancialSummaryService financialSummaryService,
            Clock clock) {
        this(financialGoalRepository, userRepository, financialSummaryService, clock, null);
    }

    public void setInvestmentRecommendationService(InvestmentRecommendationService investmentRecommendationService) {
        this.investmentRecommendationService = investmentRecommendationService;
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> list(Long userId) {
        BigDecimal monthlyLeft = monthlyLeft(userId);
        LocalDate today = LocalDate.now(clock);
        return financialGoalRepository.findByUserIdOrderByTargetDateAsc(userId).stream()
                .map(goal -> FinancialGoalResponse.from(goal, today, monthlyLeft)).toList();
    }

    @Transactional(readOnly = true)
    public FinancialGoalResponse get(Long id, Long userId) {
        return FinancialGoalResponse.from(requireOwned(id, userId), LocalDate.now(clock), monthlyLeft(userId));
    }

    @Transactional
    public FinancialGoalResponse create(Long userId, FinancialGoalRequest request) {
        validate(request, true);
        User user = userRepository.getReferenceById(userId);
        FinancialGoal goal = new FinancialGoal(user, request.name().trim(), request.goalType(), request.targetAmount(),
                request.currentAmount(), request.targetDate(), cleanDescription(request.description()));
        return FinancialGoalResponse.from(financialGoalRepository.save(goal), LocalDate.now(clock),
                monthlyLeft(userId));
    }

    @Transactional
    public FinancialGoalResponse update(Long id, Long userId, FinancialGoalRequest request) {
        validate(request, false);
        FinancialGoal goal = requireOwned(id, userId);
        goal.setName(request.name().trim());
        goal.setGoalType(request.goalType());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setTargetDate(request.targetDate());
        goal.setDescription(cleanDescription(request.description()));
        return FinancialGoalResponse.from(goal, LocalDate.now(clock), monthlyLeft(userId));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        financialGoalRepository.delete(requireOwned(id, userId));
    }

    @Transactional(readOnly = true)
    public GoalPlanResponse getPlan(Long id, Long userId) {
        FinancialGoal goal = requireOwned(id, userId);
        BigDecimal disposable = resolveDisposableIncome(userId);
        LocalDate today = LocalDate.now(clock);
        return calculatePlan(
                goal.getId(),
                goal.getName(),
                goal.getGoalType() != null ? goal.getGoalType().name() : null,
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                disposable,
                today,
                goal.getDescription());
    }

    @Transactional(readOnly = true)
    public GoalAchievementResponse getAchievement(Long id, Long userId) {
        GoalPlanResponse plan = getPlan(id, userId);
        InvestmentRecommendationResponse rec = investmentRecommendationService != null
                ? investmentRecommendationService.recommend(userId, id)
                : null;
        LocalDate today = LocalDate.now(clock);
        return calculateAchievement(plan, rec, today);
    }

    public GoalPlanResponse calculatePlan(
            Long goalId,
            String goalName,
            String goalType,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate targetDate,
            BigDecimal disposableIncome,
            LocalDate today,
            String description) {
        BigDecimal target = targetAmount != null ? targetAmount : BigDecimal.ZERO;
        BigDecimal current = currentAmount != null ? currentAmount.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal disposable = disposableIncome != null ? disposableIncome : BigDecimal.ZERO;

        BigDecimal remainingAmount;
        BigDecimal progressPercentage;
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            remainingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            progressPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (current.compareTo(target) >= 0) {
            remainingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            progressPercentage = current.multiply(new BigDecimal("100"))
                    .divide(target, 2, RoundingMode.HALF_UP);
        } else {
            remainingAmount = target.subtract(current).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            progressPercentage = current.multiply(new BigDecimal("100"))
                    .divide(target, 2, RoundingMode.HALF_UP);
        }

        long remainingMonths = 0;
        if (targetDate != null && targetDate.isAfter(today)) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(
                    today.withDayOfMonth(1), targetDate.withDayOfMonth(1));
            remainingMonths = Math.max(months, 1L);
        }

        String status;
        if (target.compareTo(BigDecimal.ZERO) > 0 && current.compareTo(target) >= 0) {
            status = "ACHIEVED";
        } else if (targetDate != null && targetDate.isBefore(today)) {
            status = "OVERDUE";
        } else if (current.compareTo(BigDecimal.ZERO) == 0) {
            status = "NOT_STARTED";
        } else {
            status = "IN_PROGRESS";
        }

        BigDecimal requiredMonthlyContribution;
        if ("ACHIEVED".equals(status) || remainingMonths <= 0 || remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            requiredMonthlyContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            requiredMonthlyContribution = remainingAmount.divide(
                    BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP);
        }

        boolean affordable;
        BigDecimal shortfallAmount;
        if (requiredMonthlyContribution.compareTo(BigDecimal.ZERO) == 0) {
            affordable = true;
            shortfallAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (disposable.compareTo(requiredMonthlyContribution) >= 0) {
            affordable = true;
            shortfallAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            affordable = false;
            shortfallAmount = requiredMonthlyContribution.subtract(disposable).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new GoalPlanResponse(
                goalId,
                goalName,
                goalType,
                target.setScale(2, RoundingMode.HALF_UP),
                current.setScale(2, RoundingMode.HALF_UP),
                targetDate,
                progressPercentage,
                remainingAmount,
                remainingMonths,
                requiredMonthlyContribution,
                disposable.setScale(2, RoundingMode.HALF_UP),
                affordable,
                shortfallAmount,
                status,
                description);
    }

    public GoalAchievementResponse calculateAchievement(
            GoalPlanResponse plan,
            InvestmentRecommendationResponse rec,
            LocalDate today) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }

        BigDecimal targetAmount = plan.targetAmount() != null
                ? plan.targetAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentAmount = plan.currentAmount() != null
                ? plan.currentAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = plan.remainingAmount() != null
                ? plan.remainingAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        LocalDate targetDate = plan.targetDate();
        BigDecimal progressPercentage = plan.progressPercentage() != null
                ? plan.progressPercentage().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long remainingMonths = plan.remainingMonths();
        BigDecimal requiredMonthlyContribution = plan.requiredMonthlyContribution() != null
                ? plan.requiredMonthlyContribution().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal disposableIncome = plan.disposableIncome() != null
                ? plan.disposableIncome().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal recommendedMonthlyInvestment = (rec != null && rec.recommendedMonthlyInvestment() != null)
                ? rec.recommendedMonthlyInvestment().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        InvestmentRiskPreference riskPreference = (rec != null && rec.riskPreference() != null)
                ? rec.riskPreference()
                : InvestmentRiskPreference.MODERATE;

        boolean isDateOverdue = targetDate != null && !targetDate.isAfter(today);

        String achievementStatus;
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            achievementStatus = "ACHIEVED";
        } else if (isDateOverdue) {
            achievementStatus = "OVERDUE";
        } else if (disposableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            achievementStatus = "NOT_FEASIBLE";
        } else if (requiredMonthlyContribution.compareTo(recommendedMonthlyInvestment) <= 0) {
            achievementStatus = "ON_TRACK";
        } else {
            achievementStatus = "AT_RISK";
        }

        boolean achievable = "ACHIEVED".equals(achievementStatus) || "ON_TRACK".equals(achievementStatus);

        BigDecimal monthlyShortfall = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlySurplus = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if ("OVERDUE".equals(achievementStatus)) {
            monthlyShortfall = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            monthlySurplus = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if ("ACHIEVED".equals(achievementStatus)) {
            monthlyShortfall = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            monthlySurplus = recommendedMonthlyInvestment.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        } else {
            if (requiredMonthlyContribution.compareTo(recommendedMonthlyInvestment) > 0) {
                monthlyShortfall = requiredMonthlyContribution.subtract(recommendedMonthlyInvestment)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (recommendedMonthlyInvestment.compareTo(requiredMonthlyContribution) > 0) {
                monthlySurplus = recommendedMonthlyInvestment.subtract(requiredMonthlyContribution)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal annualReturn = InvestmentRecommendationService.expectedReturnFor(riskPreference);
        BigDecimal projectedAmountAtTarget;
        if ("ACHIEVED".equals(achievementStatus) || "OVERDUE".equals(achievementStatus) || remainingMonths <= 0
                || recommendedMonthlyInvestment.compareTo(BigDecimal.ZERO) <= 0) {
            projectedAmountAtTarget = currentAmount;
        } else {
            BigDecimal fv = InvestmentRecommendationService.calculateFutureValue(
                    recommendedMonthlyInvestment, annualReturn, (int) remainingMonths);
            projectedAmountAtTarget = currentAmount.add(fv).setScale(2, RoundingMode.HALF_UP);
        }

        StringBuilder message = new StringBuilder();
        switch (achievementStatus) {
            case "ACHIEVED" -> message.append(String.format(
                    "Goal '%s' has already been achieved! Current savings of ₹%s meet or exceed the target amount of ₹%s.",
                    plan.goalName(), currentAmount, targetAmount));
            case "OVERDUE" -> message.append(String.format(
                    "Goal '%s' is overdue. The target date (%s) has passed with ₹%s remaining.",
                    plan.goalName(), targetDate, remainingAmount));
            case "NOT_FEASIBLE" -> message.append(String.format(
                    "Goal '%s' is currently not feasible due to zero or negative disposable income (₹%s). Investment recommendation is paused until finances improve.",
                    plan.goalName(), disposableIncome));
            case "ON_TRACK" -> message.append(String.format(
                    "Goal '%s' is on track! Recommended monthly investment of ₹%s is sufficient to meet the required monthly contribution of ₹%s.",
                    plan.goalName(), recommendedMonthlyInvestment, requiredMonthlyContribution));
            case "AT_RISK" -> message.append(String.format(
                    "Goal '%s' is at risk. Required monthly contribution of ₹%s exceeds your recommended monthly investment of ₹%s by ₹%s.",
                    plan.goalName(), requiredMonthlyContribution, recommendedMonthlyInvestment, monthlyShortfall));
            default -> message.append(String.format("Goal '%s' achievement analysis.", plan.goalName()));
        }

        if (remainingMonths > 0 && recommendedMonthlyInvestment.compareTo(BigDecimal.ZERO) > 0) {
            message.append(String.format(
                    " Projected amount at target date is ₹%s (assumed %s%% p.a. illustrative return based on %s profile; returns are not guaranteed).",
                    projectedAmountAtTarget, annualReturn.setScale(2, RoundingMode.HALF_UP), riskPreference));
        }

        List<String> warnings = new ArrayList<>();
        if (rec != null && rec.warnings() != null) {
            warnings.addAll(rec.warnings());
        }

        if ("OVERDUE".equals(achievementStatus)) {
            String overdueWarn = "Goal target date (" + targetDate + ") has passed with ₹" + remainingAmount + " remaining.";
            if (!warnings.contains(overdueWarn)) {
                warnings.add(overdueWarn);
            }
        } else if ("AT_RISK".equals(achievementStatus)) {
            String riskWarn = "Monthly shortfall of ₹" + monthlyShortfall + " to achieve '" + plan.goalName() + "' by " + targetDate + ".";
            if (!warnings.contains(riskWarn)) {
                warnings.add(riskWarn);
            }
        }

        return new GoalAchievementResponse(
                plan.goalId(),
                plan.goalName(),
                targetAmount,
                currentAmount,
                remainingAmount,
                targetDate,
                progressPercentage,
                remainingMonths,
                requiredMonthlyContribution,
                disposableIncome,
                recommendedMonthlyInvestment,
                monthlyShortfall,
                monthlySurplus,
                projectedAmountAtTarget,
                achievementStatus,
                achievable,
                message.toString(),
                warnings);
    }

    private BigDecimal resolveDisposableIncome(Long userId) {
        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, null, null);
        if (summary == null) {
            return BigDecimal.ZERO;
        }
        if (summary.disposableIncome() != null && summary.disposableIncome().compareTo(BigDecimal.ZERO) != 0) {
            return summary.disposableIncome();
        }
        java.util.Set<String> months = new java.util.HashSet<>();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        if (summary.monthlyIncomeExpense() != null) {
            for (FinancialSummaryResponse.MonthlyTypeAmount row : summary.monthlyIncomeExpense()) {
                months.add(row.year() + "-" + row.month());
                if (row.type() == com.pulsewallet.pulsewallet.entity.TransactionType.INCOME)
                    income = income.add(value(row.total()));
                if (row.type() == com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE)
                    expenses = expenses.add(value(row.total()));
            }
        }
        if (!months.isEmpty()) {
            return income.subtract(expenses).divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
        }
        return summary.disposableIncome() != null ? summary.disposableIncome() : BigDecimal.ZERO;
    }

    private FinancialGoal requireOwned(Long id, Long userId) {
        return financialGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial goal", id));
    }

    private void validate(FinancialGoalRequest request, boolean creating) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.goalType() == null || request.targetAmount() == null
                || request.currentAmount() == null || request.targetDate() == null) {
            throw new IllegalArgumentException("goal name, type, amounts, and targetDate are required");
        }
        if (request.currentAmount().compareTo(request.targetAmount()) > 0) {
            throw new IllegalArgumentException("currentAmount must not exceed targetAmount");
        }
        if (creating && !request.targetDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("targetDate must be in the future");
        }
        if (!creating && request.currentAmount().compareTo(request.targetAmount()) < 0
                && !request.targetDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("targetDate must be in the future for an incomplete goal");
        }
    }

    private BigDecimal monthlyLeft(Long userId) {
        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, null, null);
        java.util.Set<String> months = new java.util.HashSet<>();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (FinancialSummaryResponse.MonthlyTypeAmount row : summary.monthlyIncomeExpense()) {
            months.add(row.year() + "-" + row.month());
            if (row.type() == com.pulsewallet.pulsewallet.entity.TransactionType.INCOME)
                income = income.add(value(row.total()));
            if (row.type() == com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE)
                expenses = expenses.add(value(row.total()));
        }
        return months.isEmpty() ? BigDecimal.ZERO
                : income.subtract(expenses).divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String cleanDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}