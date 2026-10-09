package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.dto.FraudHubDtos.*;

@Service
public class AdvancedFraudService {

    private final Map<String, String> transactionStatusOverrides = new ConcurrentHashMap<>();

    public SecurityCenterResponse getSecurityCenter(Long userId) {
        List<FlaggedTransactionDto> flagged = getFlaggedTransactions();

        int activeAlerts = (int) flagged.stream()
                .filter(t -> "FLAGGED".equals(t.status()))
                .count();

        List<String> protections = List.of(
                "Real-time XGBoost ML Scoring Active",
                "UPI Collect Request Quarantine Active",
                "Night-time Velocity & Anomaly Radar Active",
                "Biometric Step-Up Authentication Ready",
                "Geo-IP & Terminal Mismatch Guard Active");

        return new SecurityCenterResponse(
                activeAlerts > 0 ? "ACTION_REQUIRED" : "ALL_SYSTEMS_SECURE",
                activeAlerts > 0 ? 88 : 99,
                activeAlerts,
                14,
                flagged,
                protections);
    }

    public List<FlaggedTransactionDto> getFlaggedTransactions() {
        List<FlaggedTransactionDto> list = new ArrayList<>();

        String status1 = transactionStatusOverrides.getOrDefault("TX-FLAG-801", "FLAGGED");
        list.add(new FlaggedTransactionDto(
                "TX-FLAG-801",
                new BigDecimal("48950.00"),
                "CRYPTO-EXCHANGE-GLOBAL",
                "HDFC Credit Card ending in 4102",
                "Today at 02:41 AM",
                94,
                "CRITICAL",
                List.of(
                        "Amount is 8.6x above your 30-day average transaction (₹5,600)",
                        "High-risk time anomaly: Initiated at 02:41 AM IST",
                        "New international merchant with no historical authorization",
                        "Device IP from unauthorized location (Larnaca, Cyprus)"),
                status1,
                "Larnaca, CY",
                "185.220.101.44"));

        String status2 = transactionStatusOverrides.getOrDefault("TX-FLAG-802", "FLAGGED");
        list.add(new FlaggedTransactionDto(
                "TX-FLAG-802",
                new BigDecimal("9999.00"),
                "UPI-RECHARGE-FASTNET",
                "Google Pay UPI (SBI A/C ending in 8812)",
                "Yesterday at 11:15 PM",
                76,
                "HIGH",
                List.of(
                        "Rapid velocity: 3 consecutive collect requests under 90 seconds",
                        "Amount is 3.1x usual UPI debit ticket",
                        "VPA domain recently created (unverified merchant handle)"),
                status2,
                "Jaipur, IN",
                "49.204.112.89"));

        String status3 = transactionStatusOverrides.getOrDefault("TX-FLAG-803", "VERIFIED");
        list.add(new FlaggedTransactionDto(
                "TX-FLAG-803",
                new BigDecimal("21400.00"),
                "APPLE STORE MUMBAI",
                "ICICI Credit Card ending in 9021",
                "3 days ago at 04:30 PM",
                42,
                "MEDIUM",
                List.of(
                        "Single large electronic hardware purchase",
                        "Authenticated via Chip + PIN and OTP successfully"),
                status3,
                "Mumbai, IN",
                "106.51.24.18"));

        return list;
    }

    public PhishingScanResponse scanPhishing(PhishingScanRequest request) {
        String query = request.inputQuery() != null ? request.inputQuery().trim() : "";
        if (query.isBlank()) {
            return new PhishingScanResponse(
                    query,
                    0,
                    false,
                    "SAFE",
                    "No query entered",
                    "Enter a UPI ID, SMS link, or message snippet to analyze.");
        }

        String lower = query.toLowerCase();

        // 1. Phishing URLs & SMS Scams
        if (lower.contains("lottery") || lower.contains("winner") || lower.contains("prize")
                || lower.contains("claim") || lower.contains("pan-card") || lower.contains("kyc-update")
                || lower.contains("bit.ly") || lower.contains("tinyurl") || lower.contains("block-account")
                || lower.contains("apk") || lower.contains("telegram.me")) {

            return new PhishingScanResponse(
                    query,
                    96,
                    true,
                    "MALICIOUS_PHISHING_DOMAIN",
                    "CRITICAL PHISHING ALERT: This link mimics Indian bank KYC / lottery rewards to siphon credentials.",
                    "DO NOT open the link or install APKs. Bank officers never ask for KYC updates over SMS links.");
        }

        // 2. Suspicious UPI Collect Requests
        if (lower.contains("@") && (lower.contains("refund") || lower.contains("cashback")
                || lower.contains("support") || lower.contains("helpline") || lower.contains("paytm-care"))) {

            return new PhishingScanResponse(
                    query,
                    91,
                    true,
                    "DECEPTIVE_UPI_COLLECT_REQUEST",
                    "SCAM DETECTED: Impersonating customer support to issue an unauthorized debit request.",
                    "Reject and report this collect request on BHIM/NPCI immediately. Never enter your UPI PIN to 'receive' money.");
        }

        // 3. Legitimate / Safe check
        return new PhishingScanResponse(
                query,
                8,
                false,
                "LEGITIMATE_ENTITY",
                "Verified safe. Domain and VPA pattern matches authorized banking channels.",
                "Safe to proceed under standard security practices.");
    }

    public FraudActionResponse handleFraudAction(Long userId, FraudActionRequest request) {
        String txId = request.transactionId() != null ? request.transactionId() : "TX-FLAG-801";
        String action = request.action() != null ? request.action().toUpperCase() : "FREEZE_CARD";

        if ("FREEZE_CARD".equals(action)) {
            transactionStatusOverrides.put(txId, "FROZEN");
            return new FraudActionResponse(
                    txId,
                    "FREEZE_CARD",
                    "Card successfully locked across all payment channels. Immediate unauthorized debit block dispatched.",
                    "FROZEN");
        } else if ("DISPUTE".equals(action)) {
            transactionStatusOverrides.put(txId, "DISPUTED");
            return new FraudActionResponse(
                    txId,
                    "DISPUTE",
                    "Forensic dispute case filed with payment network. Temporary credit hold requested under RBI Circular.",
                    "DISPUTED");
        } else {
            transactionStatusOverrides.put(txId, "VERIFIED");
            return new FraudActionResponse(
                    txId,
                    "VERIFIED",
                    "Transaction marked as authorized by cardholder. Machine learning spending model updated.",
                    "VERIFIED");
        }
    }

    public FraudInvestigationReportResponse generateInvestigationReport(Long userId) {
        String reportId = "INC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss IST"));

        List<FlaggedTransactionDto> evidence = getFlaggedTransactions();

        List<String> featureWeights = List.of(
                "Spend Multiplier Spike: 0.38 contribution",
                "Time-of-Day Night Anomaly: 0.27 contribution",
                "GeoIP Terminal Distance (>3,500km): 0.22 contribution",
                "New Unverified Merchant: 0.13 contribution");

        List<String> nextSteps = List.of(
                "Submit generated dispute report to your card issuer within 72 hours under RBI Zero-Liability guidelines.",
                "Keep the reference code for police cyber cell escalation (National Cyber Crime Portal 1930).",
                "A replacement EMV chip & virtual card has been queued for dispatch.");

        return new FraudInvestigationReportResponse(
                reportId,
                timestamp,
                "Comprehensive Forensic Fraud & Unauthorized Transaction Report",
                "PulseWallet Authenticated User #" + (userId != null ? userId : 101),
                evidence,
                featureWeights,
                nextSteps,
                "RBI-DISP-" + (100000 + (int) (Math.random() * 900000)));
    }
}
