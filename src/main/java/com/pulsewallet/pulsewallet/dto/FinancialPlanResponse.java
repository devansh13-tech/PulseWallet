package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;

public record FinancialPlanResponse(
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        BigDecimal monthlyLeft,
        BigDecimal goalSavingsRequired,
        BigDecimal emergencyBufferTarget,
        BigDecimal recommendedEmergencyAllocation,
        BigDecimal recommendedInvestmentAmount,
        BigDecimal flexibleSpendingAmount,
        InvestmentRiskPreference riskPreference,
        Integer timeHorizonYears,
        List<Allocation> allocation,
        List<String> warnings,
        List<String> reasons) {

    public record Allocation(String category, BigDecimal percentage, BigDecimal amount) {
    }
}
