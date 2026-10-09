package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;

public record InvestmentProjectionResponse(
        BigDecimal monthlyInvestment,
        int years,
        int months,
        BigDecimal annualExpectedReturnPercentage,
        BigDecimal monthlyExpectedReturnPercentage,
        BigDecimal totalContributed,
        BigDecimal estimatedGrowth,
        BigDecimal projectedFutureValue,
        InvestmentRiskPreference riskPreference,
        String goalName,
        BigDecimal goalTargetAmount,
        BigDecimal goalCurrentAmount,
        BigDecimal goalRemainingAmount,
        Boolean goalProjectedValueCovered,
        String projectionMessage,
        List<String> warnings) {
}
