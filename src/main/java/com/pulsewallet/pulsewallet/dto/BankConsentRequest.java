package com.pulsewallet.pulsewallet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound consent initiation request for authorized financial data access.
 */
public record BankConsentRequest(
        @NotBlank(message = "institutionId is required")
        String institutionId,

        @NotBlank(message = "consentPurpose is required")
        String consentPurpose) {
}
