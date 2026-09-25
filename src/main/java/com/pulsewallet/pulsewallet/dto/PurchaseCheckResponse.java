package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of evaluating a planned purchase against the user's credit cards
 * and overall financial capacity.
 *
 * <p>This is a financial-safety assessment only — it does <b>not</b> compare
 * reward rates or live offers across cards.
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
            List<String> reasons) {
    }
}
