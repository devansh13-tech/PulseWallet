package com.pulsewallet.pulsewallet.dto;

import java.util.List;

/**
 * Status and requirements documentation for bank-data integration provider.
 */
public record BankSyncStatusResponse(
        String frameworkName,
        boolean liveIntegrationConfigured,
        String status,
        List<String> supportedInstitutions,
        List<String> availableDataFields,
        List<String> securityRequirements,
        List<String> regulatoryRequirements,
        String notes) {
}
