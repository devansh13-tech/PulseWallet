package com.pulsewallet.pulsewallet.dto;

/**
 * Result of card identification and BIN prefix lookup.
 *
 * <p>Clearly separates confirmed vs likely information, identifies provider status,
 * and masks the card number so no full number is ever returned or stored.
 */
public record CardIdentificationResponse(
        String issuer,
        String cardNetwork,
        String productVariant,
        String confidence,
        String source,
        boolean providerConfigured,
        String maskedCardNumber,
        String binPrefix,
        String statusMessage) {
}
