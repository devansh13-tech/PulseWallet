package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record FinancialSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal disposableIncome,
        List<AmountByDate> incomeByPeriod,
        List<AmountByDate> expensesByPeriod,
        List<AmountByCategory> expensesByCategory,
        List<MonthlyAmount> monthlyTotals,
        List<CategoryMonthlyAmount> categoryMonthlyTotals) {

    public record AmountByDate(LocalDate period, BigDecimal total) {
    }

    public record AmountByCategory(Long categoryId, String categoryName, BigDecimal total) {
    }

    public record MonthlyAmount(int year, int month, BigDecimal total) {
    }

    public record CategoryMonthlyAmount(
            int year, int month, Long categoryId, String categoryName, BigDecimal total) {
    }
}
