package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * @param categoryId optional - a budget may apply across every category
 * @param endDate    validated against {@code startDate} in {@code BudgetService},
 *                   since that is a cross-field rule Bean Validation cannot
 *                   express with a single annotation here
 */
public record BudgetRequest(
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal amount,
        Long categoryId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
