package com.pulsewallet.pulsewallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pulsewallet.pulsewallet.dto.CardIdentificationRequest;
import com.pulsewallet.pulsewallet.dto.CardIdentificationResponse;
import com.pulsewallet.pulsewallet.service.cardid.CardIdentificationService;
import com.pulsewallet.pulsewallet.service.cardid.ConfigurableExternalBinProvider;
import com.pulsewallet.pulsewallet.service.cardid.LocalBinCatalogueProvider;

class CardIdentificationServiceTest {

    private CardIdentificationService service;
    private LocalBinCatalogueProvider localCatalogue;
    private ConfigurableExternalBinProvider externalProvider;

    @BeforeEach
    void setUp() {
        localCatalogue = new LocalBinCatalogueProvider();
        externalProvider = new ConfigurableExternalBinProvider("", "");
        service = new CardIdentificationService(List.of(externalProvider, localCatalogue), localCatalogue);
    }

    @Test
    void identifiesVisaCardWithLuhnValidation() {
        // Standard test Visa card passing Luhn: 4111220000000051 (sum = 20)
        String validVisa = "4111220000000051";
        CardIdentificationResponse response = service.identify(new CardIdentificationRequest(validVisa));

        assertNotNull(response);
        assertEquals("HDFC Bank", response.issuer());
        assertEquals("VISA", response.cardNetwork());
        assertEquals("Regalia Gold", response.productVariant());
        assertEquals("CONFIRMED", response.confidence());
        assertEquals("LOCAL_BIN_CATALOGUE", response.source());
        assertFalse(response.providerConfigured());
        assertEquals("•••• •••• •••• 0051", response.maskedCardNumber());
        assertEquals("411122", response.binPrefix());
    }

    @Test
    void identifiesMastercardCardWithLuhnValidation() {
        // Standard test Mastercard passing Luhn: 5181 7200 0000 0052 (sum = 20)
        String validMastercard = "5181 7200 0000 0052";
        CardIdentificationResponse response = service.identify(new CardIdentificationRequest(validMastercard));

        assertNotNull(response);
        assertEquals("State Bank of India", response.issuer());
        assertEquals("MASTERCARD", response.cardNetwork());
        assertEquals("SimplyCLICK", response.productVariant());
        assertEquals("CONFIRMED", response.confidence());
    }

    @Test
    void identifiesAmexCardWithLuhnValidation() {
        // Test Amex card: 3712 3400 0000 055 (15 digits, sum = 30)
        String validAmex = "371234000000055";
        CardIdentificationResponse response = service.identify(new CardIdentificationRequest(validAmex));

        assertNotNull(response);
        assertEquals("American Express", response.issuer());
        assertEquals("AMEX", response.cardNetwork());
        assertEquals("Platinum Travel", response.productVariant());
    }

    @Test
    void rejectsInvalidLuhnChecksum() {
        // 4111 2200 0000 0052 fails Luhn algorithm (sum = 21)
        String badChecksum = "4111220000000052";
        assertThrows(IllegalArgumentException.class, () ->
                service.identify(new CardIdentificationRequest(badChecksum)));
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () ->
                service.identify(new CardIdentificationRequest("1234")));
        assertThrows(IllegalArgumentException.class, () ->
                service.identify(new CardIdentificationRequest("1234567890123456789012345")));
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () ->
                service.identify(null));
    }

    @Test
    void masksCardNumberSafely() {
        String masked16 = CardIdentificationService.maskCardNumber(16, "1234");
        assertEquals("•••• •••• •••• 1234", masked16);

        String masked15 = CardIdentificationService.maskCardNumber(15, "9999");
        assertTrue(masked15.endsWith("9999"));
        assertFalse(masked15.contains("3712"));
    }

    @Test
    void unconfiguredExternalProviderFallsBackGracefully() {
        assertFalse(externalProvider.isConfigured());
        assertEquals("EXTERNAL_BIN_LOOKUP_PROVIDER", externalProvider.getProviderName());
        assertTrue(externalProvider.identify("411122").isEmpty());
    }
}
