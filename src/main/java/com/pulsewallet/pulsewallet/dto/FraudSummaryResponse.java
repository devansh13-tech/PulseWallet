package com.pulsewallet.pulsewallet.dto;

import java.util.List;

public record FraudSummaryResponse(
        long totalAlerts,
        long unresolvedAlerts,
        List<FraudAlertSummaryResponse> recentAlerts) {
}
