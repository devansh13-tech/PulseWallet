package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse.AiMarketInsight;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse.ChartDataPoint;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse.MarketIndexDetail;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.service.MarketIndexService;

@WebMvcTest(MarketDataController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketIndexService marketIndexService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void getIndicesReturnsOkWithMarketData() throws Exception {
        MarketIndexDetail nifty = new MarketIndexDetail(
                "^NSEI",
                "NIFTY 50",
                new BigDecimal("22520.45"),
                new BigDecimal("22603.10"),
                new BigDecimal("-82.65"),
                new BigDecimal("-0.37"),
                new BigDecimal("22580.75"),
                new BigDecimal("22294.75"),
                new BigDecimal("26373.20"),
                new BigDecimal("22179.90"),
                "BEARISH",
                List.of(new ChartDataPoint(1791517500000L, "09:15", new BigDecimal("22520.45"), new BigDecimal("-0.37"))));

        MarketIndexDetail sensex = new MarketIndexDetail(
                "^BSESN",
                "BSE SENSEX",
                new BigDecimal("72472.33"),
                new BigDecimal("72638.70"),
                new BigDecimal("-166.37"),
                new BigDecimal("-0.23"),
                new BigDecimal("72669.20"),
                new BigDecimal("71739.49"),
                new BigDecimal("86159.02"),
                new BigDecimal("71292.88"),
                "NEUTRAL",
                List.of(new ChartDataPoint(1791517500000L, "09:15", new BigDecimal("72472.33"), new BigDecimal("-0.23"))));

        AiMarketInsight aiInsight = new AiMarketInsight(
                "NEUTRAL",
                "Market is consolidating",
                "Maintain systematic investments",
                "Ensure emergency reserves are prioritized",
                List.of("NIFTY near 22520"));

        MarketIndicesResponse response = new MarketIndicesResponse(
                "CLOSED",
                "Market Closed",
                false,
                System.currentTimeMillis(),
                "1D",
                Map.of("NIFTY", nifty, "SENSEX", sensex),
                aiInsight);

        when(marketIndexService.getMarketIndices(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/market/indices").param("range", "1D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.indices.NIFTY.name").value("NIFTY 50"))
                .andExpect(jsonPath("$.data.indices.SENSEX.name").value("BSE SENSEX"))
                .andExpect(jsonPath("$.data.aiMarketInsight.sentiment").value("NEUTRAL"));
    }

    @Test
    void getMoversReturnsOkWithGainersAndLosers() throws Exception {
        com.pulsewallet.pulsewallet.dto.MarketMoversResponse.MarketMoverItem item = new com.pulsewallet.pulsewallet.dto.MarketMoversResponse.MarketMoverItem(
                "ipo-g1",
                "Waaree Energies",
                "IPO",
                "Solar PV Modules",
                new BigDecimal("2950.00"),
                new BigDecimal("1503.00"),
                new BigDecimal("96.27"),
                new BigDecimal("96.27"),
                null,
                "VERY_HIGH",
                5,
                "STRONG_BUY",
                "Leader in solar PV manufacturing",
                List.of("AGGRESSIVE"));

        com.pulsewallet.pulsewallet.dto.MarketMoversResponse moversResponse = new com.pulsewallet.pulsewallet.dto.MarketMoversResponse(
                List.of(item),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                System.currentTimeMillis());

        when(marketIndexService.getMarketMovers()).thenReturn(moversResponse);

        mockMvc.perform(get("/api/market/movers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ipoGainers[0].name").value("Waaree Energies"))
                .andExpect(jsonPath("$.data.ipoGainers[0].aiVerdict").value("STRONG_BUY"));
    }
}

