package com.pulsewallet.pulsewallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final RestClient restClient;
    private final int maxRetries;

    public FraudDetectionService(
            RestClient.Builder restClientBuilder,
            @Value("${fraud.api.url:http://127.0.0.1:8000}") String fraudApiUrl,
            @Value("${fraud.api.max-retries:2}") int maxRetries) {

        this.restClient = restClientBuilder
                .baseUrl(fraudApiUrl)
                .build();
        this.maxRetries = Math.max(0, maxRetries);
    }

    public FraudCheckResponse checkFraud(FraudCheckRequest request) {
        int retriesUsed = 0;

        while (true) {
            try {
                FraudCheckResponse response = restClient.post()
                        .uri("/fraud-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(FraudCheckResponse.class);

                if (response != null) {
                    return response;
                }

                log.warn(
                        "FastAPI fraud check returned an empty payload; returning safe fallback result instead of failing the transaction");
                return safeFallback();
            } catch (RestClientException e) {
                if (!isTransient(e) || retriesUsed >= maxRetries) {
                    log.warn(
                            "FastAPI fraud check unavailable; returning safe fallback result instead of failing the transaction. retriesUsed={}, maxRetries={}, error={}",
                            retriesUsed,
                            maxRetries,
                            e.toString());
                    return safeFallback();
                }

                retriesUsed++;
                log.warn(
                        "Transient FastAPI fraud check failure; retrying {}/{} before returning the safe fallback.",
                        retriesUsed,
                        maxRetries,
                        e);
            }
        }
    }

    private boolean isTransient(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return statusCode == 429 || statusCode >= 500;
        }

        return exception instanceof ResourceAccessException;
    }

    private FraudCheckResponse safeFallback() {
        return new FraudCheckResponse(false, 0.0, 0.0, "LOW");
    }
}