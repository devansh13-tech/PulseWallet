package com.pulsewallet.pulsewallet.service.cardid;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * External BIN provider implementation template.
 *
 * <p>Requires explicit configuration via properties (e.g. {@code card.bin.lookup.url}
 * and {@code card.bin.lookup.api-key}). When unconfigured, it safely and gracefully
 * indicates unavailability without making external requests.
 */
@Component
public class ConfigurableExternalBinProvider implements CardIdentificationProvider {

    private final String apiUrl;
    private final String apiKey;

    public ConfigurableExternalBinProvider(
            @Value("${card.bin.lookup.url:}") String apiUrl,
            @Value("${card.bin.lookup.api-key:}") String apiKey) {
        this.apiUrl = apiUrl != null ? apiUrl.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    @Override
    public String getProviderName() {
        return "EXTERNAL_BIN_LOOKUP_PROVIDER";
    }

    @Override
    public boolean isConfigured() {
        return !apiUrl.isBlank() && !apiKey.isBlank();
    }

    @Override
    public Optional<CardIdentificationResult> identify(String binPrefix) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        // Live external HTTP integration would query the external service using only binPrefix (6-8 digits).
        // Never passes full card number.
        return Optional.empty();
    }
}
