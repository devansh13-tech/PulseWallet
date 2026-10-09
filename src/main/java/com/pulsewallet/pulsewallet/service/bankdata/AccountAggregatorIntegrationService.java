package com.pulsewallet.pulsewallet.service.bankdata;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.dto.BankConsentRequest;
import com.pulsewallet.pulsewallet.dto.BankConsentResponse;
import com.pulsewallet.pulsewallet.dto.BankSyncStatusResponse;

/**
 * Concrete provider implementation representing the RBI Account Aggregator (AA)
 * framework and Financial Information User (FIU) specifications.
 *
 * <p>Documents regulatory, developer, security, and consent prerequisites for
 * live financial institution data sharing.
 */
@Service
public class AccountAggregatorIntegrationService implements BankDataIntegrationProvider {

    private final String clientKey;
    private final String clientSecret;
    private final String aaBaseUrl;

    public AccountAggregatorIntegrationService(
            @Value("${pulsewallet.bank-sync.aa.client-id:}") String clientKey,
            @Value("${pulsewallet.bank-sync.aa.client-secret:}") String clientSecret,
            @Value("${pulsewallet.bank-sync.aa.base-url:}") String aaBaseUrl) {
        this.clientKey = clientKey != null ? clientKey.trim() : "";
        this.clientSecret = clientSecret != null ? clientSecret.trim() : "";
        this.aaBaseUrl = aaBaseUrl != null ? aaBaseUrl.trim() : "";
    }

    @Override
    public String getFrameworkName() {
        return "RBI Account Aggregator (AA / FIU Specification)";
    }

    @Override
    public boolean isLiveConfigured() {
        return !clientKey.isBlank() && !clientSecret.isBlank() && !aaBaseUrl.isBlank();
    }

    @Override
    public BankSyncStatusResponse getIntegrationStatus() {
        boolean live = isLiveConfigured();
        String status = live ? "ACTIVE" : "PENDING_REGISTRATION";

        List<String> institutions = List.of(
                "HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank",
                "Kotak Mahindra Bank", "Bank of Baroda", "Punjab National Bank"
        );

        List<String> fields = List.of(
                "Masked Account Identifier", "Credit Card Outstanding Balance",
                "Available Credit Limit", "Statement Date", "Payment Due Date",
                "Minimum Due Amount", "Authorized Statement Transactions"
        );

        List<String> securityRequirements = List.of(
                "End-to-End asymmetric encryption using ephemeral DH key exchange",
                "Strict OAuth2 / OpenID Connect token based authentication",
                "Zero credential storage: PulseWallet never stores bank credentials or OTPs",
                "Explicit electronic user consent artifact with cryptographic signature"
        );

        List<String> regulatoryRequirements = List.of(
                "Registration as a licensed Financial Information User (FIU)",
                "Approval and onboarding with certified Account Aggregator (e.g. Setu, Finvu, Sahamati)",
                "Compliance with RBI master directions on Account Aggregators (2016 / 2023 updates)",
                "Periodic third-party information security and data localization audits"
        );

        String notes = live
                ? "Live Account Aggregator connector is configured and ready for user consent initiation."
                : "Live financial institution integration is in readiness mode. Credentials and FIU registration are not configured in this environment.";

        return new BankSyncStatusResponse(
                getFrameworkName(),
                live,
                status,
                institutions,
                fields,
                securityRequirements,
                regulatoryRequirements,
                notes);
    }

    @Override
    public BankConsentResponse initiateConsent(Long userId, BankConsentRequest request) {
        if (request == null || request.institutionId() == null || request.institutionId().isBlank()) {
            throw new IllegalArgumentException("institutionId is required for consent setup");
        }

        String consentHandle = "consent-handle-" + UUID.randomUUID();
        boolean live = isLiveConfigured();

        String status = live ? "AWAITING_USER_APPROVAL" : "MOCK_READINESS_NOT_CONFIGURED";
        String authUrl = live ? (aaBaseUrl + "/consent/" + consentHandle) : null;

        List<String> scopes = List.of(
                "CREDIT_CARD_STATEMENT_READ",
                "OUTSTANDING_BALANCE_READ",
                "PAYMENT_DUE_READ"
        );

        String disclaimer = "PulseWallet accesses financial data strictly through user-authorized consent handles. "
                + "We never collect or store banking passwords, NetBanking credentials, OTPs, or CVVs.";

        return new BankConsentResponse(
                consentHandle,
                request.institutionId(),
                status,
                authUrl,
                scopes,
                disclaimer);
    }

    @Override
    public List<String> getSupportedInstitutions() {
        return List.of(
                "HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank",
                "Kotak Mahindra Bank", "Bank of Baroda", "Punjab National Bank"
        );
    }
}
