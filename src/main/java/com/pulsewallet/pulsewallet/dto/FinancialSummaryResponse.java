package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.pulsewallet.pulsewallet.entity.TransactionType;

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
        List<CategoryMonthlyAmount> categoryMonthlyTotals,
        List<MonthlyTypeAmount> monthlyIncomeExpense) {

    public record AmountByDate(LocalDate period, BigDecimal total) {
    }

    public record AmountByCategory(Long categoryId, String categoryName, BigDecimal total) {
    }

    public record MonthlyAmount(int year, int month, BigDecimal total) {
    }

    public record CategoryMonthlyAmount(
            int year, int month, Long categoryId, String categoryName, BigDecimal total) {
    }

    /** Per-month income vs expense split — the data M5 needs for monthly disposable income. */
    public record MonthlyTypeAmount(int year, int month, TransactionType type, BigDecimal total) {
    }
}
