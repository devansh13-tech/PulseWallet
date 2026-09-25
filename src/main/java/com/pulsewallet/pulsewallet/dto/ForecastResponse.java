package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

public record ForecastResponse(
        int monthsIncluded,
        BigDecimal averageMonthlyExpense,
        BigDecimal forecastMonthlyExpense,
        String forecastBasis,
        List<MonthlyExpensePoint> monthlyExpenses) {

    public record MonthlyExpensePoint(
            int year,
            int month,
            BigDecimal expenseTotal) {
    }
}
