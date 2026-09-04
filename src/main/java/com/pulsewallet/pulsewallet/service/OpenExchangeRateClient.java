package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenExchangeRateClient implements ExchangeRateClient {

    private final RestClient restClient;
    private final String baseUrl;

    public OpenExchangeRateClient(
            RestClient.Builder restClientBuilder,
            @Value("${exchange-rate.base-url:https://open.er-api.com/v6/latest}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
    }

    @Override
    public Map<String, BigDecimal> fetchRates(String baseCurrency) {
        String normalizedBase = baseCurrency == null ? "INR" : baseCurrency.trim().toUpperCase();

        try {
            var response = restClient.get()
                    .uri(baseUrl + "?base={base}", normalizedBase)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, httpResponse) -> {
                        throw new IllegalStateException(
                                "Exchange rate provider returned " + httpResponse.getStatusCode());
                    })
                    .body(OpenExchangeRateResponse.class);

            if (response == null || response.rates() == null || response.rates().isEmpty()) {
                throw new IllegalStateException("Exchange rate provider returned no rates");
            }

            Map<String, BigDecimal> rates = new LinkedHashMap<>();
            rates.put(normalizedBase, BigDecimal.ONE);
            response.rates().forEach((code, value) -> rates.put(code, value));
            return rates;
        } catch (RestClientException | IllegalStateException ex) {
            throw new IllegalStateException("Unable to load exchange rates for " + normalizedBase, ex);
        }
    }

    private record OpenExchangeRateResponse(Map<String, BigDecimal> rates) {
    }
}
