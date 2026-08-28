package com.pulsewallet.pulsewallet.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;
import com.pulsewallet.pulsewallet.service.FraudDetectionService;

import jakarta.validation.Valid;

/**
 * Exposes the fraud-check capability of the Python/XGBoost microservice to
 * Spring Boot clients.
 *
 * <p>Requires a valid JWT (falls under {@code .anyRequest().authenticated()}
 * in {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/fraud")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * Analyses a transaction's 30 features for fraud.
     *
     * <p>The request is forwarded to the FastAPI fraud-detection service,
     * which applies the saved scaler and XGBoost model and returns a
     * probability, boolean result, and risk classification.
     */
    @PostMapping("/check")
    public ApiResponse<FraudCheckResponse> checkFraud(@Valid @RequestBody FraudCheckRequest request) {
        return ApiResponse.ok(fraudDetectionService.checkFraud(request));
    }
}
