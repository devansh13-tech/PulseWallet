package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Draft extraction response from a credit-card statement PDF.
 *
 * <p>Always returns with {@code requiresUserConfirmation = true} so that no
 * extracted values are automatically committed to the cardholder's record
 * without explicit review.
 */
public record StatementExtractionResponse(
        String fileName,
        String issuer,
        String cardName,
        String cardNetwork,
        String lastFourDigits,
        BigDecimal creditLimit,
        BigDecimal statementBalance,
        BigDecimal minimumDue,
        BigDecimal availableCredit,
        LocalDate statementDate,
        LocalDate dueDate,
        String billingPeriod,
        String rewardPoints,
        boolean isEncrypted,
        boolean isScanned,
        boolean requiresUserConfirmation,
        String statusMessage,
        List<ExtractedTransactionDto> transactions) {
}
