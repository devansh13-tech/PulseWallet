package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request submitted by the user after reviewing and optionally correcting
 * draft fields extracted from a statement PDF.
 */
public record StatementConfirmationRequest(
        Long cardId,

        @NotBlank(message = "issuer must not be blank")
        String issuer,

        @NotBlank(message = "cardName must not be blank")
        String cardName,

        String cardNetwork,

        @NotNull(message = "lastFourDigits is required")
        @Pattern(regexp = "\\d{4}", message = "lastFourDigits must be exactly 4 numeric digits")
        String lastFourDigits,

        @NotNull(message = "creditLimit is required")
        @DecimalMin(value = "0.01", message = "creditLimit must be greater than 0")
        BigDecimal creditLimit,

        @NotNull(message = "currentOutstanding is required")
        @DecimalMin(value = "0", message = "currentOutstanding must be zero or greater")
        BigDecimal currentOutstanding,

        @DecimalMin(value = "0", message = "statementBalance must be zero or greater")
        BigDecimal statementBalance,

        @DecimalMin(value = "0", message = "minimumDue must be zero or greater")
        BigDecimal minimumDue,

        @NotNull(message = "statementDate is required")
        LocalDate statementDate,

        @NotNull(message = "dueDate is required")
        LocalDate dueDate,

        String rewardsDescription,
        String benefitsDescription) {
}
