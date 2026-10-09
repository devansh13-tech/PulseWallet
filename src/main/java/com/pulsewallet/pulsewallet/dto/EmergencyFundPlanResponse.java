package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmergencyFundPlanResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal monthlyEssentialExpenses,
        int recommendedMonths,
        BigDecimal recommendedEmergencyFundTarget,
        BigDecimal currentEmergencyFund,
        BigDecimal remainingAmountNeeded,
        BigDecimal suggestedMonthlyContribution,
        BigDecimal disposableIncome,
        BigDecimal progressPercentage,
        List<EssentialExpenseCategory> essentialCategories) {

    public record EssentialExpenseCategory(
            String categoryName,
            BigDecimal monthlyAmount) {
    }
}
