package com.pulsewallet.pulsewallet.service.cardid;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.dto.CardIdentificationRequest;
import com.pulsewallet.pulsewallet.dto.CardIdentificationResponse;

/**
 * Service for card number validation and BIN/IIN metadata resolution.
 *
 * <p><b>Security and Privacy:</b>
 * <ul>
 *   <li>The full card number is never persisted to database or disk.</li>
 *   <li>The card number is never logged at any level.</li>
 *   <li>Only the minimum BIN/IIN prefix (6 to 8 digits) is used for identification.</li>
 *   <li>No sensitive payment credentials (CVV, PIN, OTP) are ever accepted or requested.</li>
 * </ul>
 */
@Service
public class CardIdentificationService {

    private final List<CardIdentificationProvider> providers;
    private final LocalBinCatalogueProvider localCatalogue;

    public CardIdentificationService(
            List<CardIdentificationProvider> providers,
            LocalBinCatalogueProvider localCatalogue) {
        this.providers = providers;
        this.localCatalogue = localCatalogue;
    }

    public CardIdentificationResponse identify(CardIdentificationRequest request) {
        if (request == null || request.cardNumber() == null) {
            throw new IllegalArgumentException("Card number must not be null");
        }

        String digitsOnly = request.cardNumber().replaceAll("[\\s-]", "");
        if (!digitsOnly.matches("^\\d{13,19}$")) {
            throw new IllegalArgumentException("Card number must contain between 13 and 19 numeric digits");
        }

        if (!validateLuhn(digitsOnly)) {
            throw new IllegalArgumentException("Invalid card number checksum (failed Luhn algorithm check)");
        }

        // Extract minimum 6 to 8 digits prefix for BIN identification
        int prefixLen = Math.min(8, digitsOnly.length());
        String binPrefix = digitsOnly.substring(0, prefixLen);
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        String masked = maskCardNumber(digitsOnly.length(), lastFour);

        boolean externalConfigured = providers.stream()
                .anyMatch(p -> p != null && !(p instanceof LocalBinCatalogueProvider) && p.isConfigured());

        // Try external providers first if configured
        Optional<CardIdentificationResult> resultOpt = Optional.empty();
        for (CardIdentificationProvider provider : providers) {
            if (provider != null && !(provider instanceof LocalBinCatalogueProvider) && provider.isConfigured()) {
                resultOpt = provider.identify(binPrefix);
                if (resultOpt.isPresent()) {
                    break;
                }
            }
        }

        // Fallback to local catalogue
        if (resultOpt.isEmpty()) {
            resultOpt = localCatalogue.identify(binPrefix);
        }

        CardIdentificationResult res = resultOpt.orElse(new CardIdentificationResult(
                "Unknown Issuer", "UNKNOWN", "Standard Credit Card", "UNKNOWN", "UNAVAILABLE"));

        String statusMessage = externalConfigured
                ? "Card identified via configured provider."
                : "Card identified via local BIN range catalogue (live provider not configured).";

        return new CardIdentificationResponse(
                res.issuer(),
                res.cardNetwork(),
                res.productVariant(),
                res.confidence(),
                res.source(),
                externalConfigured,
                masked,
                binPrefix.substring(0, 6),
                statusMessage);
    }

    /**
     * Standard Luhn checksum algorithm (Mod 10).
     */
    public static boolean validateLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public static String maskCardNumber(int totalLength, String lastFourDigits) {
        int maskedCount = Math.max(0, totalLength - 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maskedCount; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(' ');
            }
            sb.append('•');
        }
        sb.append(' ');
        sb.append(lastFourDigits);
        return sb.toString();
    }
}
