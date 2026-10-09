package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

public class FraudHubDtos {

    public record FlaggedTransactionDto(
            String id,
            BigDecimal amount,
            String merchant,
            String paymentChannel,
            String timestamp,
            int riskScore,
            String severity, // CRITICAL, HIGH, MEDIUM, LOW
            List<String> anomalySignals,
            String status, // FLAGGED, FROZEN, DISPUTED, VERIFIED
            String location,
            String clientIp) {}

    public record SecurityCenterResponse(
            String defenseStatus,
            int systemShieldLevel,
            int activeAlertsCount,
            int threatsNeutralizedCount,
            List<FlaggedTransactionDto> flaggedTransactions,
            List<String> activeProtections) {}

    public record PhishingScanRequest(String inputQuery) {}

    public record PhishingScanResponse(
            String query,
            int riskScore,
            boolean isScam,
            String threatType,
            String warningMessage,
            String recommendation) {}

    public record FraudActionRequest(
            String transactionId,
            String action) {} // FREEZE_CARD, DISPUTE, VERIFY

    public record FraudActionResponse(
            String transactionId,
            String actionTaken,
            String message,
            String status) {}

    public record FraudInvestigationReportResponse(
            String reportId,
            String generatedAt,
            String caseTitle,
            String userIdentifier,
            List<FlaggedTransactionDto> evidenceTrail,
            List<String> mlFeatureWeights,
            List<String> recommendedNextSteps,
            String disputeReferenceCode) {}
}
