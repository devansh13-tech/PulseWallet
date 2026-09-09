package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound request for creating or updating a credit card.
 *
 * <p><b>Security:</b> there is intentionally no field for a full card number,
 * CVV, PIN, OTP, password, or any other sensitive credential.
 * {@code lastFourDigits} is validated to be exactly four numeric digits.
 */
public record CreditCardRequest(
        @NotBlank(message = "issuer must not be blank") String issuer,
        @NotBlank(message = "cardName must not be blank") String cardName,
        @NotNull(message = "lastFourDigits is required")
        @Pattern(regexp = "\\d{4}", message = "lastFourDigits must be exactly 4 numeric digits") String lastFourDigits,
        @NotNull(message = "creditLimit is required")
        @DecimalMin(value = "0.01", message = "creditLimit must be greater than 0") BigDecimal creditLimit,
        @NotNull(message = "currentOutstanding is required")
        @DecimalMin(value = "0", message = "currentOutstanding must be zero or greater") BigDecimal currentOutstanding,
        @NotNull(message = "statementDate is required") LocalDate statementDate,
        @NotNull(message = "dueDate is required") LocalDate dueDate,
        String rewardsDescription,
        String benefitsDescription) {
}
