package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BankIngestionDtos {

    public record SmsParseRequest(String smsText) {}

    public record ParsedTransactionDto(
            BigDecimal amount,
            String type, // EXPENSE or INCOME
            String merchant,
            String paymentChannel,
            String accountMasked,
            BigDecimal balance,
            String referenceNumber,
            String suggestedCategory,
            LocalDate transactionDate,
            String rawSnippet) {}

    public record AccountAggregatorConsentRequest(
            String institutionId,
            String accountAggregatorVpa,
            String mobileNumber) {}

    public record AccountAggregatorConsentResponse(
            String consentId,
            String institutionName,
            String status,
            String aggregatorProvider, // Setu, Finvu, Anumati
            List<ParsedTransactionDto> fetchedTransactions,
            String expiresAt) {}

    public record CategoryFeedbackRequest(
            String merchant,
            String correctedCategory) {}

    public record BatchImportRequest(
            List<ParsedTransactionDto> transactions) {}
}
