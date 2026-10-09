package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.CreditCard;

/**
 * Outbound response DTO for a credit card.
 *
 * <p>Includes safe card metadata, available credit, credit utilization,
 * statement balance, minimum due, freshness indicators, and reward details.
 * Never includes sensitive credentials (full card number, CVV, PIN, OTP).
 */
public record CreditCardResponse(
        Long id,
        String issuer,
        String cardName,
        String cardNetwork,
        String lastFourDigits,
        BigDecimal creditLimit,
        BigDecimal currentOutstanding,
        BigDecimal availableCredit,
        BigDecimal creditUtilization,
        BigDecimal statementBalance,
        BigDecimal minimumDue,
        LocalDate statementDate,
        LocalDate dueDate,
        BigDecimal annualFee,
        String dataSource,
        String freshnessStatus,
        Instant lastStatementSyncAt,
        String rewardsDescription,
        String benefitsDescription,
        Instant createdAt,
        Instant updatedAt) {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final long STALE_DAYS_THRESHOLD = 35;

    /**
     * Backward-compatible 12-argument constructor.
     */
    public CreditCardResponse(
            Long id,
            String issuer,
            String cardName,
            String lastFourDigits,
            BigDecimal creditLimit,
            BigDecimal currentOutstanding,
            LocalDate statementDate,
            LocalDate dueDate,
            String rewardsDescription,
            String benefitsDescription,
            Instant createdAt,
            Instant updatedAt) {
        this(
                id,
                issuer,
                cardName,
                null,
                lastFourDigits,
                creditLimit,
                currentOutstanding,
                calculateAvailableCredit(creditLimit, currentOutstanding),
                calculateUtilization(creditLimit, currentOutstanding),
                null,
                null,
                statementDate,
                dueDate,
                BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                "MANUAL",
                "FRESH",
                null,
                rewardsDescription,
                benefitsDescription,
                createdAt,
                updatedAt);
    }

    /**
     * Backward-compatible 16-argument constructor.
     */
    public CreditCardResponse(
            Long id,
            String issuer,
            String cardName,
            String cardNetwork,
            String lastFourDigits,
            BigDecimal creditLimit,
            BigDecimal currentOutstanding,
            BigDecimal availableCredit,
            BigDecimal creditUtilization,
            LocalDate statementDate,
            LocalDate dueDate,
            BigDecimal annualFee,
            String rewardsDescription,
            String benefitsDescription,
            Instant createdAt,
            Instant updatedAt) {
        this(
                id,
                issuer,
                cardName,
                cardNetwork,
                lastFourDigits,
                creditLimit,
                currentOutstanding,
                availableCredit != null ? availableCredit : calculateAvailableCredit(creditLimit, currentOutstanding),
                creditUtilization != null ? creditUtilization : calculateUtilization(creditLimit, currentOutstanding),
                null,
                null,
                statementDate,
                dueDate,
                annualFee != null ? annualFee : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                "MANUAL",
                "FRESH",
                null,
                rewardsDescription,
                benefitsDescription,
                createdAt,
                updatedAt);
    }

    public static BigDecimal calculateAvailableCredit(BigDecimal creditLimit, BigDecimal currentOutstanding) {
        if (creditLimit == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal outstanding = currentOutstanding == null ? BigDecimal.ZERO : currentOutstanding;
        return creditLimit.subtract(outstanding).max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateUtilization(BigDecimal creditLimit, BigDecimal currentOutstanding) {
        if (creditLimit == null || creditLimit.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal outstanding = currentOutstanding == null ? BigDecimal.ZERO : currentOutstanding;
        return outstanding.multiply(HUNDRED)
                .divide(creditLimit, SCALE, RoundingMode.HALF_UP);
    }

    public static String computeFreshnessStatus(Instant lastSync, Instant updated) {
        Instant checkPoint = lastSync != null ? lastSync : updated;
        if (checkPoint == null) {
            return "FRESH";
        }
        long days = Duration.between(checkPoint, Instant.now()).toDays();
        return days > STALE_DAYS_THRESHOLD ? "STALE" : "FRESH";
    }

    public static CreditCardResponse from(CreditCard card) {
        String freshness = computeFreshnessStatus(card.getLastStatementSyncAt(), card.getUpdatedAt());
        String source = card.getDataSource() != null ? card.getDataSource() : "MANUAL";

        return new CreditCardResponse(
                card.getId(),
                card.getIssuer(),
                card.getCardName(),
                card.getCardNetwork(),
                card.getLastFourDigits(),
                card.getCreditLimit(),
                card.getCurrentOutstanding(),
                calculateAvailableCredit(card.getCreditLimit(), card.getCurrentOutstanding()),
                calculateUtilization(card.getCreditLimit(), card.getCurrentOutstanding()),
                card.getStatementBalance(),
                card.getMinimumDue(),
                card.getStatementDate(),
                card.getDueDate(),
                card.getAnnualFee() == null ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP) : card.getAnnualFee(),
                source,
                freshness,
                card.getLastStatementSyncAt(),
                card.getRewardsDescription(),
                card.getBenefitsDescription(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
