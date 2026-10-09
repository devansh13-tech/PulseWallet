package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;

public record MarketMoversResponse(
        List<MarketMoverItem> ipoGainers,
        List<MarketMoverItem> ipoLosers,
        List<MarketMoverItem> mutualFundGainers,
        List<MarketMoverItem> mutualFundLosers,
        List<MarketMoverItem> sipGainers,
        List<MarketMoverItem> sipLosers,
        long lastUpdated) {

    public record MarketMoverItem(
            String id,
            String name,
            String category, // IPO, MUTUAL_FUND, SIP
            String subCategory,
            BigDecimal currentPriceOrNav,
            BigDecimal benchmarkPrice,
            BigDecimal changePercent,
            BigDecimal return1Y,
            BigDecimal return3Y,
            String riskLevel, // HIGH, MODERATE, LOW, VERY_HIGH
            int rating, // 1 to 5
            String aiVerdict, // STRONG_BUY, ACCUMULATE, HOLD, CAUTION, AVOID, PROFIT_BOOKING
            String aiAnalysis,
            List<String> suitableProfiles) {
    }
}
