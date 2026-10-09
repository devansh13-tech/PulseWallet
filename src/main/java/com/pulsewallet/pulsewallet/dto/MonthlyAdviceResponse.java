package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;

public record MonthlyAdviceResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal disposableIncome,
        BigDecimal savingsRate,
        boolean emergencyFundReady,
        BigDecimal emergencyFundRemaining,
        BigDecimal recommendedMonthlyInvestment,
        InvestmentRiskPreference riskPreference,
        boolean diversified,
        String diversificationStatus,
        String primaryGoalName,
        String primaryGoalStatus,
        BigDecimal primaryGoalShortfall,
        String advicePriority,
        String advice,
        List<String> recommendations,
        List<String> warnings) {
}
