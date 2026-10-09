package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;

public record InvestmentRecommendationResponse(
        BigDecimal recommendedMonthlyInvestment,
        BigDecimal disposableIncome,
        BigDecimal investmentAllocationPercentage,
        boolean emergencyFundReady,
        BigDecimal emergencyFundRemaining,
        InvestmentRiskPreference riskPreference,
        BigDecimal equityPercentage,
        BigDecimal debtPercentage,
        BigDecimal goldPercentage,
        BigDecimal equityAmount,
        BigDecimal debtAmount,
        BigDecimal goldAmount,
        boolean diversified,
        String diversificationStatus,
        String diversificationMessage,
        String goalName,
        BigDecimal goalTargetAmount,
        BigDecimal goalCurrentAmount,
        BigDecimal goalRemainingAmount,
        BigDecimal goalRequiredMonthlyContribution,
        Boolean goalContributionCovered,
        String recommendationReason,
        List<String> warnings) {
}
