package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MarketIndicesResponse(
        String marketStatus,
        String marketStatusMessage,
        boolean isMarketOpen,
        long lastUpdated,
        String range,
        Map<String, MarketIndexDetail> indices,
        AiMarketInsight aiMarketInsight) {

    public record MarketIndexDetail(
            String symbol,
            String name,
            BigDecimal currentPrice,
            BigDecimal previousClose,
            BigDecimal change,
            BigDecimal changePercent,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            BigDecimal fiftyTwoWeekHigh,
            BigDecimal fiftyTwoWeekLow,
            String trend,
            List<ChartDataPoint> history) {
    }

    public record ChartDataPoint(
            long timestamp,
            String label,
            BigDecimal price,
            BigDecimal changeFromPrevClosePercent) {
    }

    public record AiMarketInsight(
            String sentiment,
            String headline,
            String recommendation,
            String assetAllocationAdvice,
            List<String> keyObservations) {
    }
}
