package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PurchaseCheckRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
        @NotNull(message = "category is required") String category,
        String purpose) {
}
