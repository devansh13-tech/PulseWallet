package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        String description,
        String merchant,
        String paymentChannel,
        Long categoryId,
        String categoryName,
        TransactionType type,
        LocalDate transactionDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TransactionResponse from(Transaction transaction) {
        Category category = transaction.getCategory();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getMerchant(),
                transaction.getPaymentChannel(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                transaction.getType(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
