package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalPlanResponse(
        Long goalId,
        String goalName,
        String goalType,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        BigDecimal progressPercentage,
        BigDecimal remainingAmount,
        long remainingMonths,
        BigDecimal requiredMonthlyContribution,
        BigDecimal disposableIncome,
        boolean affordable,
        BigDecimal shortfallAmount,
        String status,
        String description) {
}
