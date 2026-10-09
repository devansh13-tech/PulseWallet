package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GoalAchievementResponse(
        Long goalId,
        String goalName,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        LocalDate targetDate,
        BigDecimal progressPercentage,
        long remainingMonths,
        BigDecimal requiredMonthlyContribution,
        BigDecimal disposableIncome,
        BigDecimal recommendedMonthlyInvestment,
        BigDecimal monthlyShortfall,
        BigDecimal monthlySurplus,
        BigDecimal projectedAmountAtTarget,
        String achievementStatus,
        boolean achievable,
        String analysisMessage,
        List<String> warnings) {
}
