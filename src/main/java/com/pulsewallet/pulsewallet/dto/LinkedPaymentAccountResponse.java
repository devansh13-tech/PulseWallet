package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pulsewallet.pulsewallet.entity.LinkedPaymentAccount;

public record LinkedPaymentAccountResponse(
        Long id,
        String providerName,
        String accountName,
        String accountType,
        String status,
        String maskedIdentifier,
        BigDecimal balance,
        String accountSubtype,
        String ifscCode,
        Instant connectedAt,
        Instant lastSyncedAt) {

    public LinkedPaymentAccountResponse(
            Long id,
            String providerName,
            String accountName,
            String accountType,
            String status,
            String maskedIdentifier,
            Instant connectedAt,
            Instant lastSyncedAt) {
        this(id, providerName, accountName, accountType, status, maskedIdentifier, BigDecimal.ZERO, null, null, connectedAt, lastSyncedAt);
    }

    public static LinkedPaymentAccountResponse from(LinkedPaymentAccount account) {
        return new LinkedPaymentAccountResponse(
                account.getId(),
                account.getProviderName(),
                account.getAccountName(),
                account.getAccountType().name(),
                account.getStatus().name(),
                account.getMaskedIdentifier(),
                account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO,
                account.getAccountSubtype(),
                account.getIfscCode(),
                account.getConnectedAt(),
                account.getLastSyncedAt());
    }
}
