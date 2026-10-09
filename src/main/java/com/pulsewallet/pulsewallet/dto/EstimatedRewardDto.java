package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;

/**
 * Calculated reward estimation for a specific purchase or card.
 */
public record EstimatedRewardDto(
        BigDecimal pointsEarned,
        BigDecimal estimatedCashbackValue,
        BigDecimal rewardRateApplied,
        boolean isEstimate,
        BigDecimal eligibleSpend,
        String explanation,
        String source) {
}
