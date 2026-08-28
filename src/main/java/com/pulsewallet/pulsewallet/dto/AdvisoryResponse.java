package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;

public record AdvisoryResponse(
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        BigDecimal disposableIncome,
        BigDecimal emergencyFundTarget,
        BigDecimal shortTermReserveTarget,
        BigDecimal recommendedSavings,
        BigDecimal recommendedInvestment,
        String guidance) {
}
