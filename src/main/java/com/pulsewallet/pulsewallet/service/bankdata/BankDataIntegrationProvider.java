package com.pulsewallet.pulsewallet.service.bankdata;

import java.util.List;

import com.pulsewallet.pulsewallet.dto.BankConsentRequest;
import com.pulsewallet.pulsewallet.dto.BankConsentResponse;
import com.pulsewallet.pulsewallet.dto.BankSyncStatusResponse;

/**
 * Clean provider interface for future authorized open banking and
 * Account Aggregator (AA) integrations.
 *
 * <p>Strictly adheres to consent-driven, regulated financial frameworks:
 * <ul>
 *   <li>Never scrapes bank portals or collects banking passwords.</li>
 *   <li>Never asks for or handles OTPs, UPI PINs, or card CVVs.</li>
 *   <li>Requires explicit user consent and verifiable token exchange.</li>
 * </ul>
 */
public interface BankDataIntegrationProvider {

    String getFrameworkName();

    boolean isLiveConfigured();

    BankSyncStatusResponse getIntegrationStatus();

    BankConsentResponse initiateConsent(Long userId, BankConsentRequest request);

    List<String> getSupportedInstitutions();
}
