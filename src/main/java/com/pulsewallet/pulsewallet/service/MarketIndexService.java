package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse.AiMarketInsight;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse.ChartDataPoint;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse.MarketIndexDetail;
import com.pulsewallet.pulsewallet.dto.MarketMoversResponse;
import com.pulsewallet.pulsewallet.dto.MarketMoversResponse.MarketMoverItem;

@Service
public class MarketIndexService {

    private static final Logger log = LoggerFactory.getLogger(MarketIndexService.class);
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private static final String SENSEX_SYMBOL = "^BSESN";
    private static final String NIFTY_SYMBOL = "^NSEI";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final long cacheDurationMillis;

    private final Map<String, CachedMarketData> cache = new ConcurrentHashMap<>();

    public MarketIndexService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${pulsewallet.market.cache-duration-ms:15000}") long cacheDurationMillis) {
        this.restClient = restClientBuilder
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json")
                .build();
        this.objectMapper = objectMapper;
        this.cacheDurationMillis = cacheDurationMillis;
    }

    public MarketIndicesResponse getMarketIndices(String requestedRange) {
        String range = normalizeRange(requestedRange);
        long now = System.currentTimeMillis();

        CachedMarketData cached = cache.get(range);
        if (cached != null && (now - cached.timestamp() < cacheDurationMillis)) {
            return cached.data();
        }

        try {
            MarketIndexDetail sensex = fetchIndexData(SENSEX_SYMBOL, "BSE SENSEX", range);
            MarketIndexDetail nifty = fetchIndexData(NIFTY_SYMBOL, "NIFTY 50", range);

            boolean marketOpen = isIndianMarketOpen();
            String marketStatus = marketOpen ? "OPEN" : "CLOSED";
            String marketStatusMsg = marketOpen
                    ? "Live Market (Trading Session: 09:15 AM - 03:30 PM IST)"
                    : "Market Closed (Trading Session: 09:15 AM - 03:30 PM IST)";

            Map<String, MarketIndexDetail> indices = new LinkedHashMap<>();
            indices.put("NIFTY", nifty);
            indices.put("SENSEX", sensex);

            AiMarketInsight aiInsight = generateAiMarketInsight(nifty, sensex, marketOpen);

            MarketIndicesResponse response = new MarketIndicesResponse(
                    marketStatus,
                    marketStatusMsg,
                    marketOpen,
                    now,
                    range,
                    indices,
                    aiInsight);

            cache.put(range, new CachedMarketData(now, response));
            return response;
        } catch (Exception ex) {
            log.warn("Failed to fetch live market indices from Yahoo Finance ({}), falling back to procedural market feed", ex.getMessage());
            return getFallbackResponse(range);
        }
    }

    public MarketMoversResponse getMarketMovers() {
        long now = System.currentTimeMillis();

        List<MarketMoverItem> ipoGainers = List.of(
                new MarketMoverItem("ipo-g1", "Waaree Energies", "IPO", "Solar PV Modules", new BigDecimal("2950.00"), new BigDecimal("1503.00"), new BigDecimal("96.27"), new BigDecimal("96.27"), null, "VERY_HIGH", 5, "STRONG_BUY", "Leading solar PV manufacturer with robust global export order book.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-g2", "Premier Energies", "IPO", "Solar Cell & Tech", new BigDecimal("1120.00"), new BigDecimal("450.00"), new BigDecimal("148.89"), new BigDecimal("148.89"), null, "VERY_HIGH", 5, "HOLD", "Beneficiary of renewable energy PLI scheme; trade with trailing stop.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-g3", "IREDA", "IPO", "Green Energy NBFC", new BigDecimal("210.00"), new BigDecimal("32.00"), new BigDecimal("556.25"), new BigDecimal("135.50"), null, "HIGH", 5, "ACCUMULATE", "Pure-play green financing PSU financing India's renewable transition.", List.of("AGGRESSIVE", "MODERATE")),
                new MarketMoverItem("ipo-g4", "Jyoti CNC Automation", "IPO", "CNC & Aerospace", new BigDecimal("1180.00"), new BigDecimal("331.00"), new BigDecimal("256.50"), new BigDecimal("165.20"), null, "HIGH", 5, "HOLD", "Multi-year aerospace order backlog; hold for multi-cap growth.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-g5", "Tata Technologies", "IPO", "Automotive ER&D", new BigDecimal("960.00"), new BigDecimal("500.00"), new BigDecimal("92.00"), new BigDecimal("12.50"), null, "MODERATE", 4, "ACCUMULATE", "High brand pedigree and key EV software design engineering client base.", List.of("AGGRESSIVE", "MODERATE")),
                new MarketMoverItem("ipo-g6", "Bajaj Housing Finance", "IPO", "Housing NBFC", new BigDecimal("138.00"), new BigDecimal("70.00"), new BigDecimal("97.14"), new BigDecimal("97.14"), null, "MODERATE", 5, "STRONG_BUY", "Pristine asset quality with strong parentage support across retail mortgages.", List.of("MODERATE", "CONSERVATIVE", "AGGRESSIVE")),
                new MarketMoverItem("ipo-g7", "KRN Heat Exchanger", "IPO", "HVAC Components", new BigDecimal("485.00"), new BigDecimal("220.00"), new BigDecimal("120.45"), new BigDecimal("120.45"), null, "VERY_HIGH", 4, "PROFIT_BOOKING", "Super-normal listing return; prudent to lock in partial profits.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-g8", "BLS E-Services", "IPO", "Digital Governance", new BigDecimal("245.00"), new BigDecimal("135.00"), new BigDecimal("81.48"), new BigDecimal("81.48"), null, "HIGH", 4, "HOLD", "Expanding digital citizen facilitation centers across Tier-2/3 India.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-g9", "Unicommerce eSolutions", "IPO", "E-Commerce SaaS", new BigDecimal("195.00"), new BigDecimal("108.00"), new BigDecimal("80.56"), new BigDecimal("80.56"), null, "HIGH", 4, "ACCUMULATE", "Fast growing e-commerce fulfillment and supply-chain tech stack.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-g10", "Bansal Wire Industries", "IPO", "Precision Wire", new BigDecimal("445.00"), new BigDecimal("256.00"), new BigDecimal("73.83"), new BigDecimal("73.83"), null, "MODERATE", 4, "HOLD", "High industrial capacity utilization supporting manufacturing upside.", List.of("MODERATE", "AGGRESSIVE"))
        );

        List<MarketMoverItem> ipoLosers = List.of(
                new MarketMoverItem("ipo-l1", "Paytm (One97)", "IPO", "FinTech Payments", new BigDecimal("715.00"), new BigDecimal("2150.00"), new BigDecimal("-66.74"), new BigDecimal("-12.40"), new BigDecimal("-48.20"), "VERY_HIGH", 2, "CAUTION", "Regulatory restructuring underway; await consistent quarterly EBITDA turnaround.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-l2", "CarTrade Tech", "IPO", "Auto Marketplace", new BigDecimal("920.00"), new BigDecimal("1618.00"), new BigDecimal("-43.14"), new BigDecimal("35.20"), new BigDecimal("-22.50"), "HIGH", 2, "AVOID", "Moderate used-car auction volumes; margin expansion remains gradual.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-l3", "Star Health Insurance", "IPO", "Standalone Health Ins", new BigDecimal("470.00"), new BigDecimal("900.00"), new BigDecimal("-47.78"), new BigDecimal("-18.50"), new BigDecimal("-38.20"), "HIGH", 3, "CAUTION", "Elevated medical inflation and claims ratio affecting underwriting profit.", List.of("MODERATE")),
                new MarketMoverItem("ipo-l4", "Delhivery", "IPO", "Express Logistics", new BigDecimal("340.00"), new BigDecimal("487.00"), new BigDecimal("-30.18"), new BigDecimal("-8.40"), new BigDecimal("-28.90"), "HIGH", 3, "ACCUMULATE", "Turnaround in partial-truckload operations makes current price an attractive dip.", List.of("AGGRESSIVE", "MODERATE")),
                new MarketMoverItem("ipo-l5", "Ola Electric Mobility", "IPO", "EV Two-Wheelers", new BigDecimal("54.00"), new BigDecimal("76.00"), new BigDecimal("-28.95"), new BigDecimal("-28.95"), null, "VERY_HIGH", 2, "AVOID", "Service capacity hurdles and subsidy phase-out impact near-term gross margin.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-l6", "Go Fashion (India)", "IPO", "Apparel Retail", new BigDecimal("510.00"), new BigDecimal("690.00"), new BigDecimal("-26.09"), new BigDecimal("-14.20"), new BigDecimal("-18.60"), "MODERATE", 3, "HOLD", "Discretionary retail footfall normalization; sustainable store economics.", List.of("MODERATE")),
                new MarketMoverItem("ipo-l7", "Honasa Consumer (Mamaearth)", "IPO", "D2C Personal Care", new BigDecimal("255.00"), new BigDecimal("324.00"), new BigDecimal("-21.30"), new BigDecimal("-32.50"), null, "HIGH", 2, "CAUTION", "General trade transition caused inventory rationalization; high marketing costs.", List.of("AGGRESSIVE")),
                new MarketMoverItem("ipo-l8", "Medi Assist Healthcare", "IPO", "Third Party Admin", new BigDecimal("375.00"), new BigDecimal("418.00"), new BigDecimal("-10.29"), new BigDecimal("-10.29"), null, "MODERATE", 3, "ACCUMULATE", "Dominant corporate health benefits administrator trading at fair valuations.", List.of("MODERATE", "CONSERVATIVE")),
                new MarketMoverItem("ipo-l9", "Aadhar Housing Finance", "IPO", "Affordable Housing", new BigDecimal("295.00"), new BigDecimal("315.00"), new BigDecimal("-6.35"), new BigDecimal("-6.35"), null, "MODERATE", 4, "STRONG_BUY", "Solid tier-4/5 low ticket housing loan asset quality with high spreads.", List.of("MODERATE", "CONSERVATIVE")),
                new MarketMoverItem("ipo-l10", "LIC of India", "IPO", "Life Insurance PSU", new BigDecimal("910.00"), new BigDecimal("949.00"), new BigDecimal("-4.11"), new BigDecimal("28.50"), new BigDecimal("12.40"), "LOW", 4, "HOLD", "Unrivaled agency network with steady dividend yield cushion.", List.of("CONSERVATIVE", "MODERATE"))
        );

        List<MarketMoverItem> mutualFundGainers = List.of(
                new MarketMoverItem("mf-g1", "Motilal Oswal Midcap Fund", "MUTUAL_FUND", "Mid Cap Direct-G", new BigDecimal("118.45"), new BigDecimal("79.80"), new BigDecimal("48.43"), new BigDecimal("48.43"), new BigDecimal("36.50"), "VERY_HIGH", 5, "STRONG_BUY", "Focused high-conviction midcap strategy generating top-decile alpha.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-g2", "Quant Small Cap Fund", "MUTUAL_FUND", "Small Cap Direct-G", new BigDecimal("284.10"), new BigDecimal("199.30"), new BigDecimal("42.55"), new BigDecimal("42.55"), new BigDecimal("34.20"), "VERY_HIGH", 5, "ACCUMULATE", "Dynamic quantitative risk model navigating market cycles actively.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-g3", "Nippon India Small Cap Fund", "MUTUAL_FUND", "Small Cap Direct-G", new BigDecimal("174.80"), new BigDecimal("126.10"), new BigDecimal("38.62"), new BigDecimal("38.62"), new BigDecimal("32.80"), "VERY_HIGH", 5, "STRONG_BUY", "Extensive diversification across 180+ holdings limits individual stock drag.", List.of("AGGRESSIVE", "MODERATE")),
                new MarketMoverItem("mf-g4", "HDFC Mid-Cap Opportunities", "MUTUAL_FUND", "Mid Cap Direct-G", new BigDecimal("198.25"), new BigDecimal("145.60"), new BigDecimal("36.16"), new BigDecimal("36.16"), new BigDecimal("29.40"), "HIGH", 5, "STRONG_BUY", "Long-standing fund with proven risk-adjusted compounding record.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("mf-g5", "Bandhan Small Cap Fund", "MUTUAL_FUND", "Small Cap Direct-G", new BigDecimal("48.70"), new BigDecimal("36.00"), new BigDecimal("35.28"), new BigDecimal("35.28"), new BigDecimal("27.90"), "VERY_HIGH", 4, "ACCUMULATE", "High-growth stock selection with agile tactical cash deployment.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-g6", "SBI Contra Fund", "MUTUAL_FUND", "Contra / Value Direct-G", new BigDecimal("392.40"), new BigDecimal("291.70"), new BigDecimal("34.52"), new BigDecimal("34.52"), new BigDecimal("28.10"), "HIGH", 5, "STRONG_BUY", "Disciplined contrarian investing purchasing mispriced sector leaders.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("mf-g7", "ICICI Prudential Technology", "MUTUAL_FUND", "Tech Sectoral Direct-G", new BigDecimal("218.60"), new BigDecimal("163.40"), new BigDecimal("33.78"), new BigDecimal("33.78"), new BigDecimal("24.60"), "VERY_HIGH", 4, "ACCUMULATE", "Direct participation in IT services recovery and enterprise cloud/AI adoption.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-g8", "Tata Digital India Fund", "MUTUAL_FUND", "Thematic IT Direct-G", new BigDecimal("58.90"), new BigDecimal("44.80"), new BigDecimal("31.47"), new BigDecimal("31.47"), new BigDecimal("23.80"), "VERY_HIGH", 4, "HOLD", "Concentrated tech allocations with good participation in mid-tier software firms.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-g9", "Mirae Asset Large & Midcap", "MUTUAL_FUND", "Large & Mid Cap Direct-G", new BigDecimal("152.30"), new BigDecimal("118.60"), new BigDecimal("28.41"), new BigDecimal("28.41"), new BigDecimal("23.20"), "HIGH", 4, "STRONG_BUY", "Well-balanced blend of bluechip stability and mid-cap alpha generation.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("mf-g10", "Parag Parikh Flexi Cap Fund", "MUTUAL_FUND", "Flexi Cap Direct-G", new BigDecimal("84.20"), new BigDecimal("66.50"), new BigDecimal("26.62"), new BigDecimal("26.62"), new BigDecimal("22.80"), "MODERATE", 5, "STRONG_BUY", "Flagship long-term core holding with domestic value and international hedge.", List.of("CONSERVATIVE", "MODERATE", "AGGRESSIVE"))
        );

        List<MarketMoverItem> mutualFundLosers = List.of(
                new MarketMoverItem("mf-l1", "Edelweiss Greater China Off-Shore", "MUTUAL_FUND", "International FoF", new BigDecimal("34.80"), new BigDecimal("39.40"), new BigDecimal("-11.68"), new BigDecimal("-11.68"), new BigDecimal("-14.20"), "VERY_HIGH", 2, "AVOID", "Persistent headwinds in Chinese equity indices; prefer domestic Indian equity.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-l2", "Nippon India ETF Hang Seng BeES", "MUTUAL_FUND", "Overseas ETF", new BigDecimal("22.10"), new BigDecimal("24.15"), new BigDecimal("-8.49"), new BigDecimal("-8.49"), new BigDecimal("-12.50"), "VERY_HIGH", 2, "AVOID", "Hong Kong benchmark volatility; avoid concentrated exposure.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-l3", "DSP World Mining Fund", "MUTUAL_FUND", "Thematic Commodities", new BigDecimal("28.50"), new BigDecimal("30.40"), new BigDecimal("-6.25"), new BigDecimal("-6.25"), new BigDecimal("-8.90"), "VERY_HIGH", 2, "CAUTION", "Commodity cyclical deceleration impacting international mining majors.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-l4", "Franklin Asian Equity Fund", "MUTUAL_FUND", "Regional Equity FoF", new BigDecimal("38.20"), new BigDecimal("39.80"), new BigDecimal("-4.02"), new BigDecimal("-4.02"), new BigDecimal("-6.80"), "HIGH", 2, "CAUTION", "Currency fluctuations and Asian market underperformance vs India.", List.of("MODERATE")),
                new MarketMoverItem("mf-l5", "Kotak Global Emerging Market", "MUTUAL_FUND", "Global Feeder FoF", new BigDecimal("26.40"), new BigDecimal("27.45"), new BigDecimal("-3.83"), new BigDecimal("-3.83"), new BigDecimal("-5.60"), "HIGH", 2, "AVOID", "High dollar strength creates sustained drag on emerging market returns.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-l6", "Tata Clean Energy Opportunities", "MUTUAL_FUND", "Clean Tech Thematic", new BigDecimal("19.40"), new BigDecimal("20.05"), new BigDecimal("-3.24"), new BigDecimal("-3.24"), new BigDecimal("6.50"), "VERY_HIGH", 3, "HOLD", "Global solar module oversupply creating margin headwinds for pureplays.", List.of("AGGRESSIVE")),
                new MarketMoverItem("mf-l7", "Aditya Birla SL Banking & Fin", "MUTUAL_FUND", "Financial Sectoral", new BigDecimal("68.20"), new BigDecimal("69.90"), new BigDecimal("-2.43"), new BigDecimal("-2.43"), new BigDecimal("7.20"), "HIGH", 3, "ACCUMULATE", "Deposit cost plateauing offers attractive valuation for banking accumulation.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("mf-l8", "UTI Transport and Logistics", "MUTUAL_FUND", "Automotive Thematic", new BigDecimal("31.80"), new BigDecimal("32.40"), new BigDecimal("-1.85"), new BigDecimal("-1.85"), new BigDecimal("11.20"), "HIGH", 3, "HOLD", "Short term inventory correction post festive season; secular growth intact.", List.of("MODERATE")),
                new MarketMoverItem("mf-l9", "Invesco India Gilt Fund", "MUTUAL_FUND", "Sovereign Debt", new BigDecimal("28.10"), new BigDecimal("27.75"), new BigDecimal("1.26"), new BigDecimal("1.26"), new BigDecimal("4.80"), "LOW", 3, "CAUTION", "Fixed-income yield curve flatness; trailed prevailing consumer inflation.", List.of("CONSERVATIVE")),
                new MarketMoverItem("mf-l10", "Axis Focused 25 Fund", "MUTUAL_FUND", "Focused 25 Direct-G", new BigDecimal("52.40"), new BigDecimal("51.15"), new BigDecimal("2.44"), new BigDecimal("2.44"), new BigDecimal("8.10"), "HIGH", 2, "AVOID", "Consecutive multi-year underperformance against the Nifty 50 benchmark.", List.of("MODERATE"))
        );

        List<MarketMoverItem> sipGainers = List.of(
                new MarketMoverItem("sip-g1", "Motilal Oswal Midcap (SIP)", "SIP", "3Y SIP XIRR: +39.2%", new BigDecimal("582000.00"), new BigDecimal("360000.00"), new BigDecimal("61.67"), new BigDecimal("46.80"), new BigDecimal("39.20"), "VERY_HIGH", 5, "STRONG_BUY", "₹10,000/month grew to ₹5.82 Lakhs (3Y SIP return +61.7%).", List.of("AGGRESSIVE")),
                new MarketMoverItem("sip-g2", "Quant Small Cap (SIP)", "SIP", "3Y SIP XIRR: +37.6%", new BigDecimal("568000.00"), new BigDecimal("360000.00"), new BigDecimal("57.78"), new BigDecimal("41.50"), new BigDecimal("37.60"), "VERY_HIGH", 5, "STRONG_BUY", "₹10,000/month grew to ₹5.68 Lakhs through systematic small cap compounding.", List.of("AGGRESSIVE")),
                new MarketMoverItem("sip-g3", "Nippon India Small Cap (SIP)", "SIP", "3Y SIP XIRR: +34.8%", new BigDecimal("545000.00"), new BigDecimal("360000.00"), new BigDecimal("51.39"), new BigDecimal("37.20"), new BigDecimal("34.80"), "VERY_HIGH", 5, "STRONG_BUY", "₹10,000/month grew to ₹5.45 Lakhs; broad diversification cushioned dips.", List.of("AGGRESSIVE", "MODERATE")),
                new MarketMoverItem("sip-g4", "HDFC Mid-Cap Opps (SIP)", "SIP", "3Y SIP XIRR: +31.4%", new BigDecimal("518000.00"), new BigDecimal("360000.00"), new BigDecimal("43.89"), new BigDecimal("34.80"), new BigDecimal("31.40"), "HIGH", 5, "STRONG_BUY", "₹10,000/month grew to ₹5.18 Lakhs with low portfolio churn.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("sip-g5", "Kotak Emerging Equity (SIP)", "SIP", "3Y SIP XIRR: +28.2%", new BigDecimal("493000.00"), new BigDecimal("360000.00"), new BigDecimal("36.94"), new BigDecimal("31.20"), new BigDecimal("28.20"), "HIGH", 5, "STRONG_BUY", "Consistent midcap SIP performer; ideal for 5+ year wealth targets.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("sip-g6", "Bandhan Core Equity (SIP)", "SIP", "3Y SIP XIRR: +26.8%", new BigDecimal("482000.00"), new BigDecimal("360000.00"), new BigDecimal("33.89"), new BigDecimal("29.50"), new BigDecimal("26.80"), "HIGH", 4, "ACCUMULATE", "Balanced large & midcap systematic allocator with steady trajectory.", List.of("MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("sip-g7", "SBI Large & Midcap (SIP)", "SIP", "3Y SIP XIRR: +25.4%", new BigDecimal("472000.00"), new BigDecimal("360000.00"), new BigDecimal("31.11"), new BigDecimal("27.80"), new BigDecimal("25.40"), "HIGH", 4, "STRONG_BUY", "High liquidity portfolio suitable for disciplined salaried investors.", List.of("MODERATE", "CONSERVATIVE")),
                new MarketMoverItem("sip-g8", "Parag Parikh Flexi Cap (SIP)", "SIP", "3Y SIP XIRR: +24.9%", new BigDecimal("468000.00"), new BigDecimal("360000.00"), new BigDecimal("30.00"), new BigDecimal("25.80"), "24.90" != null ? new BigDecimal("24.90") : null, "MODERATE", 5, "STRONG_BUY", "Core portfolio SIP anchor with steady drawdown control and downside shield.", List.of("CONSERVATIVE", "MODERATE", "AGGRESSIVE")),
                new MarketMoverItem("sip-g9", "ICICI Pru Bluechip (SIP)", "SIP", "3Y SIP XIRR: +22.1%", new BigDecimal("448000.00"), new BigDecimal("360000.00"), new BigDecimal("24.44"), new BigDecimal("23.40"), new BigDecimal("22.10"), "MODERATE", 5, "STRONG_BUY", "India's largest bluechip equity fund; bedrock of long term retirement goals.", List.of("CONSERVATIVE", "MODERATE")),
                new MarketMoverItem("sip-g10", "UTI Nifty 50 Index (SIP)", "SIP", "3Y SIP XIRR: +17.8%", new BigDecimal("418000.00"), new BigDecimal("360000.00"), new BigDecimal("16.11"), new BigDecimal("19.20"), new BigDecimal("17.80"), "LOW", 5, "STRONG_BUY", "Ultra-low cost index compounding capturing India's top 50 corporate leaders.", List.of("CONSERVATIVE", "MODERATE", "AGGRESSIVE"))
        );

        List<MarketMoverItem> sipLosers = List.of(
                new MarketMoverItem("sip-l1", "China/Hong Kong Feeder SIPs", "SIP", "3Y SIP XIRR: -9.8%", new BigDecimal("312000.00"), new BigDecimal("360000.00"), new BigDecimal("-13.33"), new BigDecimal("-11.20"), new BigDecimal("-9.80"), "VERY_HIGH", 2, "AVOID", "SIP resulted in ₹48,000 capital loss due to prolonged property contraction.", List.of("AGGRESSIVE")),
                new MarketMoverItem("sip-l2", "Global Natural Resources Feeder", "SIP", "3Y SIP XIRR: -4.2%", new BigDecimal("338000.00"), new BigDecimal("360000.00"), new BigDecimal("-6.11"), new BigDecimal("-5.80"), new BigDecimal("-4.20"), "VERY_HIGH", 2, "AVOID", "Commodity cycles punish continuous SIPs; cyclical lump-sum timing is required.", List.of("AGGRESSIVE")),
                new MarketMoverItem("sip-l3", "Long Duration 10-Yr G-Sec SIP", "SIP", "3Y SIP XIRR: +3.8%", new BigDecimal("381000.00"), new BigDecimal("360000.00"), new BigDecimal("5.83"), new BigDecimal("2.10"), new BigDecimal("3.80"), "LOW", 3, "CAUTION", "Return severely trailed retail inflation; inefficient for aggressive goals.", List.of("CONSERVATIVE")),
                new MarketMoverItem("sip-l4", "Liquid Cash Parking SIPs", "SIP", "3Y SIP XIRR: +5.4%", new BigDecimal("391000.00"), new BigDecimal("360000.00"), new BigDecimal("8.61"), new BigDecimal("6.20"), new BigDecimal("5.40"), "LOW", 3, "CAUTION", "High cash drag; use liquid funds for STP transfers, not multi-year SIPs.", List.of("CONSERVATIVE")),
                new MarketMoverItem("sip-l5", "Low-Yield Arbitrage SIPs", "SIP", "3Y SIP XIRR: +5.9%", new BigDecimal("394000.00"), new BigDecimal("360000.00"), new BigDecimal("9.44"), new BigDecimal("6.50"), new BigDecimal("5.90"), "LOW", 3, "HOLD", "Safe tax-efficient emergency reserve, but sub-optimal for wealth goals.", List.of("CONSERVATIVE")),
                new MarketMoverItem("sip-l6", "Conservative Debt Hybrid SIPs", "SIP", "3Y SIP XIRR: +6.2%", new BigDecimal("396000.00"), new BigDecimal("360000.00"), new BigDecimal("10.00"), new BigDecimal("7.10"), new BigDecimal("6.20"), "MODERATE", 3, "CAUTION", "High tax drag post debt-fund amendment; equity savings funds offer better yield.", List.of("CONSERVATIVE")),
                new MarketMoverItem("sip-l7", "Specialty Chemicals Sector SIPs", "SIP", "3Y SIP XIRR: +6.9%", new BigDecimal("401000.00"), new BigDecimal("360000.00"), new BigDecimal("11.39"), new BigDecimal("-2.40"), new BigDecimal("6.90"), "HIGH", 3, "HOLD", "Global Chinese dumping created margins contraction; long term turnaround pending.", List.of("AGGRESSIVE")),
                new MarketMoverItem("sip-l8", "ESG Thematic Fund SIPs", "SIP", "3Y SIP XIRR: +7.8%", new BigDecimal("405000.00"), new BigDecimal("360000.00"), new BigDecimal("12.50"), new BigDecimal("8.40"), new BigDecimal("7.80"), "HIGH", 2, "AVOID", "Restrictive portfolio mandates underperformed the broad Nifty 50 by over 10%.", List.of("MODERATE")),
                new MarketMoverItem("sip-l9", "Active Large Cap Laggard SIPs", "SIP", "3Y SIP XIRR: +10.2%", new BigDecimal("412000.00"), new BigDecimal("360000.00"), new BigDecimal("14.44"), new BigDecimal("11.80"), new BigDecimal("10.20"), "MODERATE", 2, "AVOID", "High expense ratio dragged performance below Nifty 50 passive index funds.", List.of("MODERATE")),
                new MarketMoverItem("sip-l10", "Clean Energy Sectoral SIPs", "SIP", "3Y SIP XIRR: +6.5%", new BigDecimal("398000.00"), new BigDecimal("360000.00"), new BigDecimal("10.56"), new BigDecimal("-1.80"), new BigDecimal("6.50"), "VERY_HIGH", 3, "HOLD", "High capital expenditure gestation; avoid over-allocating beyond 5% of portfolio.", List.of("AGGRESSIVE"))
        );

        return new MarketMoversResponse(
                ipoGainers,
                ipoLosers,
                mutualFundGainers,
                mutualFundLosers,
                sipGainers,
                sipLosers,
                now);
    }

    private String normalizeRange(String range) {
        if (range == null) {
            return "1D";
        }
        String upper = range.trim().toUpperCase();
        return switch (upper) {
            case "1W", "5D" -> "1W";
            case "1M", "1MO" -> "1M";
            case "1Y" -> "1Y";
            default -> "1D";
        };
    }

    private MarketIndexDetail fetchIndexData(String symbol, String name, String range) throws Exception {
        String yRange;
        String yInterval;

        switch (range) {
            case "1W" -> {
                yRange = "5d";
                yInterval = "15m";
            }
            case "1M" -> {
                yRange = "1mo";
                yInterval = "1d";
            }
            case "1Y" -> {
                yRange = "1y";
                yInterval = "1wk";
            }
            default -> {
                yRange = "1d";
                yInterval = "5m";
            }
        }

        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?range=" + yRange + "&interval=" + yInterval;

        String rawJson = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode resultNode = root.path("chart").path("result").get(0);
        if (resultNode == null || resultNode.isMissingNode()) {
            throw new IllegalStateException("Missing chart result for " + symbol);
        }

        JsonNode meta = resultNode.path("meta");
        BigDecimal currentPrice = toBigDecimal(meta.path("regularMarketPrice").asDouble());
        BigDecimal prevClose = toBigDecimal(meta.path("previousClose").asDouble(meta.path("chartPreviousClose").asDouble()));
        if (prevClose.compareTo(BigDecimal.ZERO) == 0) {
            prevClose = currentPrice;
        }

        BigDecimal dayHigh = toBigDecimal(meta.path("regularMarketDayHigh").asDouble(currentPrice.doubleValue()));
        BigDecimal dayLow = toBigDecimal(meta.path("regularMarketDayLow").asDouble(currentPrice.doubleValue()));
        BigDecimal fiftyTwoWeekHigh = toBigDecimal(meta.path("fiftyTwoWeekHigh").asDouble());
        BigDecimal fiftyTwoWeekLow = toBigDecimal(meta.path("fiftyTwoWeekLow").asDouble());

        BigDecimal change = currentPrice.subtract(prevClose).setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = prevClose.compareTo(BigDecimal.ZERO) != 0
                ? change.multiply(BigDecimal.valueOf(100)).divide(prevClose, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String trend = "NEUTRAL";
        if (changePercent.compareTo(new BigDecimal("0.25")) >= 0) {
            trend = "BULLISH";
        } else if (changePercent.compareTo(new BigDecimal("-0.25")) <= 0) {
            trend = "BEARISH";
        }

        JsonNode timestamps = resultNode.path("timestamp");
        JsonNode quotes = resultNode.path("indicators").path("quote").get(0).path("close");

        List<ChartDataPoint> history = new ArrayList<>();
        if (timestamps.isArray() && quotes.isArray()) {
            DateTimeFormatter formatter = getFormatterForRange(range);
            int size = Math.min(timestamps.size(), quotes.size());
            BigDecimal lastValidPrice = currentPrice;

            for (int i = 0; i < size; i++) {
                JsonNode quoteVal = quotes.get(i);
                if (quoteVal != null && !quoteVal.isNull() && quoteVal.isNumber()) {
                    long ts = timestamps.get(i).asLong();
                    BigDecimal price = BigDecimal.valueOf(quoteVal.asDouble()).setScale(2, RoundingMode.HALF_UP);
                    lastValidPrice = price;
                    ZonedDateTime zdt = Instant.ofEpochSecond(ts).atZone(IST_ZONE);
                    String label = zdt.format(formatter);

                    BigDecimal pointChangePct = prevClose.compareTo(BigDecimal.ZERO) != 0
                            ? price.subtract(prevClose).multiply(BigDecimal.valueOf(100)).divide(prevClose, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    history.add(new ChartDataPoint(ts * 1000, label, price, pointChangePct));
                }
            }
        }

        // If history is empty, create at least two points (prevClose and currentPrice)
        if (history.isEmpty()) {
            long nowSec = System.currentTimeMillis() / 1000;
            ZonedDateTime zdt = Instant.ofEpochSecond(nowSec).atZone(IST_ZONE);
            String label = zdt.format(getFormatterForRange(range));
            history.add(new ChartDataPoint((nowSec - 3600) * 1000, "Open", prevClose, BigDecimal.ZERO));
            history.add(new ChartDataPoint(nowSec * 1000, label, currentPrice, changePercent));
        }

        return new MarketIndexDetail(
                symbol,
                name,
                currentPrice,
                prevClose,
                change,
                changePercent,
                dayHigh,
                dayLow,
                fiftyTwoWeekHigh,
                fiftyTwoWeekLow,
                trend,
                history);
    }

    private DateTimeFormatter getFormatterForRange(String range) {
        return switch (range) {
            case "1W" -> DateTimeFormatter.ofPattern("EEE dd MMM HH:mm");
            case "1M" -> DateTimeFormatter.ofPattern("dd MMM");
            case "1Y" -> DateTimeFormatter.ofPattern("MMM yyyy");
            default -> DateTimeFormatter.ofPattern("HH:mm");
        };
    }

    private BigDecimal toBigDecimal(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isIndianMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(IST_ZONE);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        LocalTime marketOpen = LocalTime.of(9, 15);
        LocalTime marketClose = LocalTime.of(15, 30);
        return !time.isBefore(marketOpen) && !time.isAfter(marketClose);
    }

    private AiMarketInsight generateAiMarketInsight(MarketIndexDetail nifty, MarketIndexDetail sensex, boolean isMarketOpen) {
        BigDecimal avgChangePct = nifty.changePercent().add(sensex.changePercent())
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        String sentiment;
        String headline;
        String recommendation;
        String assetAllocationAdvice;

        if (avgChangePct.compareTo(new BigDecimal("1.00")) >= 0) {
            sentiment = "BULLISH";
            headline = "Indian equity markets are exhibiting strong bullish momentum (NIFTY "
                    + formatPct(nifty.changePercent()) + ", SENSEX " + formatPct(sensex.changePercent()) + ").";
            recommendation = "Maintain regular Systematic Investment Plan (SIP) contributions without pausing. Market runs reward consistent discipline over speculative market timing.";
            assetAllocationAdvice = "For moderate to aggressive profiles, keep your target equity allocation active. If your equity holding exceeds your target threshold, consider rebalancing gains into debt/emergency buffers.";
        } else if (avgChangePct.compareTo(new BigDecimal("0.10")) >= 0) {
            sentiment = "MODERATELY_BULLISH";
            headline = "Indian indices are holding steady gains in positive territory (NIFTY "
                    + formatPct(nifty.changePercent()) + ", SENSEX " + formatPct(sensex.changePercent()) + ").";
            recommendation = "A steady market is ideal for automated rupee-cost averaging. Continue deploying surplus savings toward index and diversified mutual funds.";
            assetAllocationAdvice = "Ensure you maintain 3–6 months in emergency reserves so short-term index volatility never interrupts your wealth creation goals.";
        } else if (avgChangePct.compareTo(new BigDecimal("-0.10")) > 0) {
            sentiment = "NEUTRAL";
            headline = "Indian indices are consolidating in a tight sideways range (NIFTY "
                    + formatPct(nifty.changePercent()) + ", SENSEX " + formatPct(sensex.changePercent()) + ").";
            recommendation = "Consolidation phases are the best foundation for long-term compounding. Maintain your scheduled allocations and review budget targets.";
            assetAllocationAdvice = "Stick to your personalized 50-30-20 or rule-engine budget allocation without making abrupt adjustments based on intraday noise.";
        } else if (avgChangePct.compareTo(new BigDecimal("-1.00")) >= 0) {
            sentiment = "CAUTIOUS";
            headline = "Indian equity benchmarks are experiencing mild pullback pressure (NIFTY "
                    + formatPct(nifty.changePercent()) + ", SENSEX " + formatPct(sensex.changePercent()) + ").";
            recommendation = "Market pullbacks offer attractive valuations for SIP investors. Avoid panic-selling; rupee-cost averaging acquires more fund units at discounted NAVs.";
            assetAllocationAdvice = "Keep core capital focused on your prioritized financial goals. Never liquidate long-term equity holdings for short-term consumption.";
        } else {
            sentiment = "BEARISH";
            headline = "Broad market indexes are witnessing corrective selling (NIFTY "
                    + formatPct(nifty.changePercent()) + ", SENSEX " + formatPct(sensex.changePercent()) + ").";
            recommendation = "Sharp corrections test investor psychology. Historical data demonstrates that staying invested through downturns drives the highest long-term annualized returns.";
            assetAllocationAdvice = "Verify that your emergency fund is fully capitalized so you never have to tap equity portfolios during a temporary market downturn.";
        }

        List<String> keyObservations = List.of(
                "NIFTY 50 trading at " + nifty.currentPrice() + " (" + formatPct(nifty.changePercent()) + " change)",
                "BSE SENSEX trading at " + sensex.currentPrice() + " (" + formatPct(sensex.changePercent()) + " change)",
                isMarketOpen ? "Active trading session in progress (NSE / BSE live feed)" : "Markets closed — last closing benchmarks displayed",
                "Historical 52-week Nifty range: " + nifty.fiftyTwoWeekLow() + " - " + nifty.fiftyTwoWeekHigh());

        return new AiMarketInsight(
                sentiment,
                headline,
                recommendation,
                assetAllocationAdvice,
                keyObservations);
    }

    private String formatPct(BigDecimal pct) {
        if (pct.compareTo(BigDecimal.ZERO) >= 0) {
            return "+" + pct + "%";
        }
        return pct + "%";
    }

    private MarketIndicesResponse getFallbackResponse(String range) {
        long now = System.currentTimeMillis();
        boolean marketOpen = isIndianMarketOpen();
        String status = marketOpen ? "OPEN" : "CLOSED";
        String statusMsg = marketOpen
                ? "Live Market (Trading Session: 09:15 AM - 03:30 PM IST)"
                : "Market Closed (Trading Session: 09:15 AM - 03:30 PM IST)";

        // Generate high-fidelity baseline data
        BigDecimal niftyBase = new BigDecimal("22520.45");
        BigDecimal niftyPrev = new BigDecimal("22603.10");
        BigDecimal niftyChange = niftyBase.subtract(niftyPrev).setScale(2, RoundingMode.HALF_UP);
        BigDecimal niftyChangePct = new BigDecimal("-0.37");

        BigDecimal sensexBase = new BigDecimal("72472.33");
        BigDecimal sensexPrev = new BigDecimal("72638.70");
        BigDecimal sensexChange = sensexBase.subtract(sensexPrev).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sensexChangePct = new BigDecimal("-0.23");

        List<ChartDataPoint> niftyHistory = generateProceduralHistory(niftyBase, niftyPrev, range);
        List<ChartDataPoint> sensexHistory = generateProceduralHistory(sensexBase, sensexPrev, range);

        MarketIndexDetail nifty = new MarketIndexDetail(
                NIFTY_SYMBOL,
                "NIFTY 50",
                niftyBase,
                niftyPrev,
                niftyChange,
                niftyChangePct,
                new BigDecimal("22580.75"),
                new BigDecimal("22294.75"),
                new BigDecimal("26373.20"),
                new BigDecimal("22179.90"),
                "NEUTRAL",
                niftyHistory);

        MarketIndexDetail sensex = new MarketIndexDetail(
                SENSEX_SYMBOL,
                "BSE SENSEX",
                sensexBase,
                sensexPrev,
                sensexChange,
                sensexChangePct,
                new BigDecimal("72669.20"),
                new BigDecimal("71739.49"),
                new BigDecimal("86159.02"),
                new BigDecimal("71292.88"),
                "NEUTRAL",
                sensexHistory);

        Map<String, MarketIndexDetail> indices = new LinkedHashMap<>();
        indices.put("NIFTY", nifty);
        indices.put("SENSEX", sensex);

        AiMarketInsight aiInsight = generateAiMarketInsight(nifty, sensex, marketOpen);

        return new MarketIndicesResponse(
                status,
                statusMsg,
                marketOpen,
                now,
                range,
                indices,
                aiInsight);
    }

    private List<ChartDataPoint> generateProceduralHistory(BigDecimal currentPrice, BigDecimal prevClose, String range) {
        List<ChartDataPoint> points = new ArrayList<>();
        int count = switch (range) {
            case "1W" -> 20;
            case "1M" -> 22;
            case "1Y" -> 24;
            default -> 25;
        };

        long nowSec = System.currentTimeMillis() / 1000;
        long stepSec = switch (range) {
            case "1W" -> 86400 / 4;
            case "1M" -> 86400;
            case "1Y" -> 86400 * 15;
            default -> 900;
        };

        DateTimeFormatter formatter = getFormatterForRange(range);
        double base = prevClose.doubleValue();
        double target = currentPrice.doubleValue();

        for (int i = 0; i < count; i++) {
            double progress = (double) i / (count - 1);
            double noise = Math.sin(progress * Math.PI * 3) * (base * 0.003);
            double val = base + (target - base) * progress + noise;
            BigDecimal price = BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
            long ts = (nowSec - (count - 1 - i) * stepSec) * 1000;
            String label = Instant.ofEpochMilli(ts).atZone(IST_ZONE).format(formatter);
            BigDecimal changePct = BigDecimal.valueOf((val - base) / base * 100).setScale(2, RoundingMode.HALF_UP);
            points.add(new ChartDataPoint(ts, label, price, changePct));
        }

        return points;
    }

    private record CachedMarketData(long timestamp, MarketIndicesResponse data) {
    }
}
