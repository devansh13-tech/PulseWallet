package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full Credit Card Intelligence report for an authenticated user.
 *
 * <p>All thresholds and statuses are PulseWallet's internal planning
 * thresholds and are <b>not</b> official credit-bureau classifications.
 */
public record CreditCardIntelligenceResponse(
        List<CardHealth> cards,
        CreditCardSummary summary,
        RepaymentCapacity repaymentCapacity,
        List<String> warnings,
        List<String> recommendations) {

    public record CardHealth(
            Long id,
            String cardName,
            String issuer,
            String lastFourDigits,
            BigDecimal creditLimit,
            BigDecimal currentOutstanding,
            BigDecimal availableCredit,
            BigDecimal utilizationPercentage,
            String utilizationLevel,
            String healthStatus,
            LocalDate statementDate,
            LocalDate dueDate,
            String rewardsDescription,
            String benefitsDescription,
            List<String> warnings,
            List<String> recommendations) {
    }

    public record CreditCardSummary(
            BigDecimal totalCreditLimit,
            BigDecimal totalOutstanding,
            BigDecimal totalAvailableCredit,
            BigDecimal overallUtilization,
            int numberOfCards,
            int cardsNeedingAttention,
            int cardsWithUpcomingDueDate) {
    }

    public record RepaymentCapacity(
            String status,
            BigDecimal monthlyLeft,
            BigDecimal availableAfterPlan,
            BigDecimal totalOutstanding,
            List<String> explanation) {
    }
}
