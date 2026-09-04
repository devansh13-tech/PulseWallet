package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "INR", "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "AED");

    private final ExchangeRateClient exchangeRateClient;
    private final String baseCurrency;
    private final long cacheDurationMillis;
    private final Map<String, CacheEntry> rateCache = new ConcurrentHashMap<>();

    public ExchangeRateService(
            ExchangeRateClient exchangeRateClient,
            @Value("${pulsewallet.currency.base:INR}") String baseCurrency,
            @Value("${pulsewallet.exchange-rate.cache-duration-ms:1800000}") long cacheDurationMillis) {
        this.exchangeRateClient = exchangeRateClient;
        this.baseCurrency = normalizeCurrency(baseCurrency);
        this.cacheDurationMillis = cacheDurationMillis;
    }

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        String source = normalizeCurrency(fromCurrency);
        String target = normalizeCurrency(toCurrency);

        if (source.equals(target) || !isSupported(source) || !isSupported(target)) {
            return amount;
        }

        BigDecimal sourceRate = getRate(source, target);
        return amount.multiply(sourceRate).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> getRates(String base) {
        String normalizedBase = normalizeCurrency(base);
        if (!isSupported(normalizedBase)) {
            Map<String, BigDecimal> fallback = new LinkedHashMap<>();
            fallback.put(normalizedBase, new BigDecimal("1.00"));
            return fallback;
        }

        CacheEntry cached = rateCache.get(normalizedBase);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timestampMillis() < cacheDurationMillis)) {
            return cached.rates();
        }

        try {
            Map<String, BigDecimal> freshRates = exchangeRateClient.fetchRates(normalizedBase);
            Map<String, BigDecimal> normalized = normalizeRates(freshRates, normalizedBase);
            rateCache.put(normalizedBase, new CacheEntry(normalized, now));
            return normalized;
        } catch (Exception ex) {
            if (cached != null) {
                return cached.rates();
            }
            Map<String, BigDecimal> fallback = new LinkedHashMap<>();
            fallback.put(normalizedBase, new BigDecimal("1.00"));
            return fallback;
        }
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    private BigDecimal getRate(String source, String target) {
        Map<String, BigDecimal> sourceRates = getRates(source);
        BigDecimal sourceRate = sourceRates.get(source);
        BigDecimal targetRate = sourceRates.get(target);

        if (sourceRate == null || targetRate == null) {
            return BigDecimal.ONE;
        }

        return targetRate.divide(sourceRate, 12, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> normalizeRates(Map<String, BigDecimal> rates, String base) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        normalized.put(base, new BigDecimal("1.00"));
        if (rates == null) {
            return normalized;
        }
        rates.forEach((currencyCode, rate) -> {
            String key = normalizeCurrency(currencyCode);
            if (SUPPORTED_CURRENCIES.contains(key) && rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                normalized.put(key, rate);
            }
        });
        return normalized;
    }

    private boolean isSupported(String currency) {
        return currency != null && SUPPORTED_CURRENCIES.contains(currency);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return baseCurrency;
        }
        String normalized = currency.trim().toUpperCase();
        return SUPPORTED_CURRENCIES.contains(normalized) ? normalized : baseCurrency;
    }

    private record CacheEntry(Map<String, BigDecimal> rates, long timestampMillis) {
    }
}
