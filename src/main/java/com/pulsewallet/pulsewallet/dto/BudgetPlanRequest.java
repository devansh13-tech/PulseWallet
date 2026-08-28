package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BudgetPlanRequest(
        @NotNull @DecimalMin(value = "0.01", message = "salary must be greater than 0") BigDecimal salary,

        @NotNull LocalDate startDate,

        @NotNull LocalDate endDate) {
}