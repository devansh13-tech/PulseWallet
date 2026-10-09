package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of evaluating a planned purchase against the user's credit cards
 * and overall financial capacity.
 */
public record PurchaseCheckResponse(
        BigDecimal amount,
        String category,
        String purpose,
        String financialSafetyStatus,
        BigDecimal currentMonthlyLeft,
        BigDecimal availableAfterPlan,
        List<CardAssessment> cardAssessments,
        List<String> warnings,
        List<String> recommendations) {

    public record CardAssessment(
            Long cardId,
            String cardName,
            String issuer,
            String lastFourDigits,
            BigDecimal creditLimit,
            BigDecimal currentOutstanding,
            BigDecimal availableCredit,
            BigDecimal projectedOutstanding,
            BigDecimal projectedUtilization,
            String safetyStatus,
            List<String> reasons,
            EstimatedRewardDto estimatedReward) {

        public CardAssessment(
                Long cardId,
                String cardName,
                String issuer,
                String lastFourDigits,
                BigDecimal creditLimit,
                BigDecimal currentOutstanding,
                BigDecimal availableCredit,
                BigDecimal projectedOutstanding,
                BigDecimal projectedUtilization,
                String safetyStatus,
                List<String> reasons) {
            this(cardId, cardName, issuer, lastFourDigits, creditLimit, currentOutstanding,
                    availableCredit, projectedOutstanding, projectedUtilization, safetyStatus, reasons, null);
        }
    }
}
