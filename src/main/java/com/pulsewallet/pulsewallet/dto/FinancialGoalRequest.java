package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.FinancialGoalType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinancialGoalRequest(
        @NotBlank(message = "name must not be blank") String name,
        @NotNull FinancialGoalType goalType,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal targetAmount,
        @NotNull @DecimalMin(value = "0", message = "must be zero or greater") BigDecimal currentAmount,
        @NotNull LocalDate targetDate,
        String description) {
}