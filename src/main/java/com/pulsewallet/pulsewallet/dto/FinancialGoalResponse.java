package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.FinancialGoal;

public record FinancialGoalResponse(
        Long id,
        String name,
        String goalType,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        long remainingMonths,
        BigDecimal requiredMonthlySaving,
        String status,
        LocalDate targetDate,
        Instant createdAt,
        String description) {

    public static FinancialGoalResponse from(FinancialGoal goal, LocalDate today, BigDecimal monthlyLeft) {
        BigDecimal target = goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount();
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);
        BigDecimal progress = current.multiply(BigDecimal.valueOf(100))
                .divide(target, 2, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
        long months = remainingMonths(today, goal.getTargetDate());
        BigDecimal required = remaining.signum() == 0 ? BigDecimal.ZERO
                : remaining.divide(BigDecimal.valueOf(Math.max(months, 1)), 2, java.math.RoundingMode.HALF_UP);
        String status = current.compareTo(target) >= 0 ? "COMPLETED"
                : goal.getTargetDate().isBefore(today) ? "OVERDUE"
                        : required.compareTo(monthlyLeft) > 0 ? "NEEDS_ATTENTION" : "ON_TRACK";

        return new FinancialGoalResponse(goal.getId(), goal.getName(), goal.getGoalType().name(), target, current,
                remaining, progress, months, required, status, goal.getTargetDate(), goal.getCreatedAt(),
                goal.getDescription());
    }

    private static long remainingMonths(LocalDate today, LocalDate targetDate) {
        if (!targetDate.isAfter(today))
            return 0;
        long months = java.time.temporal.ChronoUnit.MONTHS.between(
                today.withDayOfMonth(1), targetDate.withDayOfMonth(1));
        return Math.max(months, 1);
    }
}