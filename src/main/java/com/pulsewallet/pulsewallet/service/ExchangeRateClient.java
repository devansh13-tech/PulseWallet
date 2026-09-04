package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateClient {
    Map<String, BigDecimal> fetchRates(String baseCurrency);
}
