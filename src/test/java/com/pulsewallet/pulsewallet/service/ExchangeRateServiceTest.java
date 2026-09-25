package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(exchangeRateClient, "INR", 60_000L);
    }

    @Test
    void convert_sameCurrency_returnsSameValue() {
        BigDecimal amount = new BigDecimal("100.00");

        BigDecimal result = exchangeRateService.convert(amount, "INR", "INR");

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void convert_inrToUsd_usesCurrentRate() {
        when(exchangeRateClient.fetchRates("INR")).thenReturn(Map.of(
                "INR", new BigDecimal("1.00"),
                "USD", new BigDecimal("0.012")));

        BigDecimal result = exchangeRateService.convert(new BigDecimal("100.00"), "INR", "USD");

        assertThat(result).isEqualByComparingTo("1.20");
    }

    @Test
    void convert_usdToInr_usesCurrentRate() {
        when(exchangeRateClient.fetchRates("USD")).thenReturn(Map.of(
                "USD", new BigDecimal("1.00"),
                "INR", new BigDecimal("83.33")));

        BigDecimal result = exchangeRateService.convert(new BigDecimal("100.00"), "USD", "INR");

        assertThat(result).isEqualByComparingTo("8333.00");
    }

    @Test
    void getRates_whenProviderFails_usesCachedFallback() {
        ExchangeRateService expiredCacheService = new ExchangeRateService(exchangeRateClient, "INR", 0L);
        when(exchangeRateClient.fetchRates("INR")).thenReturn(Map.of(
                "INR", new BigDecimal("1.00"),
                "USD", new BigDecimal("0.012"),
                "EUR", new BigDecimal("0.009")));

        expiredCacheService.getRates("INR");
        when(exchangeRateClient.fetchRates("INR")).thenThrow(new IllegalStateException("provider down"));

        Map<String, BigDecimal> result = expiredCacheService.getRates("INR");

        assertThat(result).containsEntry("INR", new BigDecimal("1.00"));
        assertThat(result).containsEntry("USD", new BigDecimal("0.012"));
    }

    @Test
    void getRates_whenProviderFailsAndNoCache_returnsBaseOnlyRates() {
        when(exchangeRateClient.fetchRates("INR")).thenThrow(new IllegalStateException("provider down"));

        Map<String, BigDecimal> result = exchangeRateService.getRates("INR");

        assertThat(result).containsEntry("INR", new BigDecimal("1.00"));
        assertThat(result).hasSize(1);
    }

    @Test
    void convert_invalidCurrency_returnsOriginalAmount() {
        BigDecimal amount = new BigDecimal("100.00");

        BigDecimal result = exchangeRateService.convert(amount, "XYZ", "USD");

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void convert_zeroAndNegativeValues_areHandledWithoutCrashing() {
        when(exchangeRateClient.fetchRates("INR")).thenReturn(Map.of(
                "INR", new BigDecimal("1.00"),
                "USD", new BigDecimal("0.012")));

        assertThat(exchangeRateService.convert(BigDecimal.ZERO, "INR", "USD")).isZero();
        assertThat(exchangeRateService.convert(new BigDecimal("-100.00"), "INR", "USD"))
                .isEqualByComparingTo("-1.20");
    }
}
