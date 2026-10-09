package com.pulsewallet.pulsewallet.service.cardid;

/**
 * Internal provider lookup result.
 */
public record CardIdentificationResult(
        String issuer,
        String cardNetwork,
        String productVariant,
        String confidence,
        String source) {
}
