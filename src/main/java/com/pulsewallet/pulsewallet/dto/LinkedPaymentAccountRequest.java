package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;

import com.pulsewallet.pulsewallet.entity.LinkedPaymentAccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LinkedPaymentAccountRequest(
        @NotBlank(message = "providerName must not be blank") String providerName,
        @NotBlank(message = "accountName must not be blank") String accountName,
        @NotNull(message = "accountType is required") LinkedPaymentAccountType accountType,
        @NotBlank(message = "maskedIdentifier must not be blank") String maskedIdentifier,
        BigDecimal balance,
        String accountSubtype,
        String ifscCode) {

    public LinkedPaymentAccountRequest(String providerName, String accountName,
            LinkedPaymentAccountType accountType, String maskedIdentifier) {
        this(providerName, accountName, accountType, maskedIdentifier, BigDecimal.ZERO, null, null);
    }
}
