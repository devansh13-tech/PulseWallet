package com.pulsewallet.pulsewallet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound request for card number identification and BIN lookup.
 *
 * <p><b>Security:</b> Card number is strictly transient: it is never saved to the database,
 * never logged, and only the minimum BIN/IIN prefix is used for identification.
 */
public record CardIdentificationRequest(
        @NotBlank(message = "cardNumber is required")
        @Pattern(regexp = "^[0-9\\s-]{13,23}$", message = "cardNumber must contain 13 to 19 digits")
        @JsonAlias({"card_number", "number"})
        String cardNumber) {
}
