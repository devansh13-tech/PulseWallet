package com.pulsewallet.pulsewallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON returned by the Python fraud-detection microservice.
 *
 * <p>FastAPI/Pydantic serialises with snake_case by default, so every field
 * needs an explicit {@link JsonProperty} to bridge the naming gap.
 */
public record FraudCheckResponse(
        @JsonProperty("is_fraud") boolean isFraud,
        @JsonProperty("fraud_probability") double fraudProbability,
        @JsonProperty("risk_score") double riskScore,
        @JsonProperty("risk_level") String riskLevel) {
}