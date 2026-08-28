package com.pulsewallet.pulsewallet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;

@Service
public class FraudDetectionService {

    private final RestClient restClient;

    public FraudDetectionService(
            RestClient.Builder restClientBuilder,
            @Value("${fraud.api.url:http://127.0.0.1:8000}") String fraudApiUrl) {

        this.restClient = restClientBuilder
                .baseUrl(fraudApiUrl)
                .build();
    }

    public FraudCheckResponse checkFraud(FraudCheckRequest request) {

        return restClient.post()
                .uri("/fraud-check")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FraudCheckResponse.class);
    }
}