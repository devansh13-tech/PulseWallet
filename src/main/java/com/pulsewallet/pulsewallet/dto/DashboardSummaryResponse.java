package com.pulsewallet.pulsewallet.dto;

import java.util.List;

public record DashboardSummaryResponse(
        FinancialSummaryResponse financialSummary,
        List<BudgetResponse> budgets,
        FraudSummaryResponse fraudSummary) {
}
