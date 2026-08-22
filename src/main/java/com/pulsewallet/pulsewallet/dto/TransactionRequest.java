package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param categoryId optional - a transaction may be uncategorized. */
public record TransactionRequest(
                @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal amount,
                @Size(max = 255) String description,
                Long categoryId,
                @NotNull TransactionType type,
                @NotNull LocalDate transactionDate,
                @Size(max = 160) String merchant,
                @Size(max = 40) String paymentChannel) {

        public TransactionRequest(
                        BigDecimal amount,
                        String description,
                        Long categoryId,
                        TransactionType type,
                        LocalDate transactionDate) {
                this(amount, description, categoryId, type, transactionDate, null, null);
        }
}
