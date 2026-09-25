package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.Budget;
import com.pulsewallet.pulsewallet.entity.Category;

public record BudgetResponse(
        Long id,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt) {

    public static BudgetResponse from(Budget budget) {
        Category category = budget.getCategory();
        return new BudgetResponse(
                budget.getId(),
                budget.getAmount(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                budget.getStartDate(),
                budget.getEndDate(),
                budget.getCreatedAt(),
                budget.getUpdatedAt());
    }
}
