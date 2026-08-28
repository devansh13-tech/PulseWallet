package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;

public record BudgetPlanResponse(
        BigDecimal salary,
        BigDecimal totalExpenses,
        BigDecimal disposableIncome,
        BigDecimal recommendedSavings,
        BigDecimal recommendedInvestment,
        BigDecimal recommendedSpending) {
}