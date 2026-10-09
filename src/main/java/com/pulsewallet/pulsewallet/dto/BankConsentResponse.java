package com.pulsewallet.pulsewallet.dto;

import java.util.List;

/**
 * Details of bank-data consent setup or readiness status.
 */
public record BankConsentResponse(
        String consentHandle,
        String institutionId,
        String status,
        String authorizationUrl,
        List<String> consentScopes,
        String disclaimer) {
}
