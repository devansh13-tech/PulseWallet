package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pulsewallet.pulsewallet.entity.FraudAlert;

public record FraudAlertSummaryResponse(
        Long id,
        Long transactionId,
        String riskLevel,
        BigDecimal fraudProbability,
        BigDecimal riskScore,
        boolean resolved,
        Instant createdAt) {

    public static FraudAlertSummaryResponse from(FraudAlert alert) {
        return new FraudAlertSummaryResponse(
                alert.getId(),
                alert.getTransaction() != null ? alert.getTransaction().getId() : null,
                alert.getRiskLevel() != null ? alert.getRiskLevel().name() : null,
                alert.getFraudProbability(),
                alert.getRiskScore(),
                alert.isResolved(),
                alert.getCreatedAt());
    }
}
