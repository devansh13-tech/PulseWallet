package com.pulsewallet.pulsewallet.entity;

/**
 * Whether money is coming in or going out.
 *
 * <p>Shared by {@link Category} (a category is either an income category or
 * an expense category) and {@link Transaction} (every transaction is one or
 * the other). Persisted as its {@code name()} string via
 * {@code @Enumerated(EnumType.STRING)} - never {@code ORDINAL}, which breaks
 * silently if a value is ever reordered.
 */
public enum TransactionType {
    INCOME,
    EXPENSE
}
