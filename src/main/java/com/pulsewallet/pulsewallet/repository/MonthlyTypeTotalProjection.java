package com.pulsewallet.pulsewallet.repository;

import java.math.BigDecimal;

import com.pulsewallet.pulsewallet.entity.TransactionType;

/**
 * Projection for monthly totals split by transaction type (INCOME vs EXPENSE).
 * Used by the financial summary to provide the data M5's financial planning
 * engine will consume for per-month income-versus-expense breakdowns.
 */
public interface MonthlyTypeTotalProjection {
    Integer getYear();

    Integer getMonth();

    TransactionType getType();

    BigDecimal getTotal();
}
