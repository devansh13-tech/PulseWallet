package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Card benefit and rewards rule metadata response.
 */
public record CardBenefitsResponse(
        Long cardId,
        String issuer,
        String cardName,
        BigDecimal publishedAnnualFee,
        BigDecimal baseRewardRatePercent,
        Map<String, BigDecimal> categoryMultipliers,
        List<String> excludedCategories,
        List<String> publishedBenefits,
        String source,
        boolean isVerified) {
}
