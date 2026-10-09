package com.pulsewallet.pulsewallet.service.cardid;

import java.util.Optional;

/**
 * Pluggable provider interface for BIN/IIN identification.
 *
 * <p>Only the minimum required prefix (6 to 8 digits) is ever passed to providers.
 * Full card numbers are strictly forbidden.
 */
public interface CardIdentificationProvider {

    String getProviderName();

    boolean isConfigured();

    Optional<CardIdentificationResult> identify(String binPrefix);
}
