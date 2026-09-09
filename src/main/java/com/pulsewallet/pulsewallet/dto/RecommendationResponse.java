package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(
        BigDecimal monthlyIncome,
        BigDecimal totalExpenses,
        BigDecimal monthlyLeft,
        BigDecimal monthlySavings,
        BigDecimal investments,
        BigDecimal goalSavings,
        BigDecimal buffer,
        BigDecimal flexibleSpending,
        List<RecommendationItem> recommendations,
        String engine) {

    public record RecommendationItem(
            String type,
            String title,
            String message,
            String priority,
            BigDecimal relatedAmount,
            String relatedGoal,
            String financialImpact,
            String reason) {
    }
}