package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.FinancialHubDtos.*;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.FinancialHubService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(FinancialHubController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class FinancialHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialHubService hubService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private UserPrincipal createPrincipal(Long userId) {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), userId);
        return new UserPrincipal(user);
    }

    @Test
    void getInsights_returnsDailySafeToSpendAndSubscriptions() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        ProactiveInsightsResponse response = new ProactiveInsightsResponse(
                new BigDecimal("2140.00"),
                "COMFORTABLE",
                11,
                new BigDecimal("25000.00"),
                List.of(),
                new BigDecimal("3500.00"),
                List.of(),
                List.of("Safe spending zone: 42% budget cushion"));

        when(hubService.getProactiveInsights(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/financial-hub/proactive-insights")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.safeToSpendToday").value(2140.00))
                .andExpect(jsonPath("$.data.daysRemainingInMonth").value(11));
    }

    @Test
    void getCibilScore_returnsScoreAndRating() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        CibilCreditScoreResponse response = new CibilCreditScoreResponse(
                784,
                "EXCELLENT",
                "Experian & CIBIL Partners",
                100,
                18,
                4.5,
                1,
                6,
                List.of("100% on-time payments", "Credit utilization under 20%"),
                List.of(750, 765, 784));

        when(hubService.getCibilCreditScore(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/financial-hub/cibil-score")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(784))
                .andExpect(jsonPath("$.data.scoreBand").value("EXCELLENT"));
    }

    @Test
    void getDebtPayoff_returnsSnowballAndAvalancheComparison() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        DebtPayoffComparisonResponse response = new DebtPayoffComparisonResponse(
                "AVALANCHE",
                14,
                new BigDecimal("18450.00"),
                11,
                new BigDecimal("14200.00"),
                new BigDecimal("4250.00"),
                List.of());

        when(hubService.getDebtPayoffComparison(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/financial-hub/debt-payoff")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.snowballMonthsToDebtFree").value(14))
                .andExpect(jsonPath("$.data.avalancheMonthsToDebtFree").value(11))
                .andExpect(jsonPath("$.data.recommendedStrategy").value("AVALANCHE"));
    }

    @Test
    void getNetWorth_returnsAggregatedAssetsAndLiabilities() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        NetWorthBreakdownResponse response = new NetWorthBreakdownResponse(
                new BigDecimal("1245000.00"),
                new BigDecimal("1545000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("380000.00"),
                new BigDecimal("650000.00"),
                new BigDecimal("420000.00"),
                new BigDecimal("95000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("250000.00"),
                0.19);

        when(hubService.getNetWorthBreakdown(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/financial-hub/net-worth")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNetWorth").value(1245000.00))
                .andExpect(jsonPath("$.data.totalLiabilities").value(300000.00));
    }

    @Test
    void getTaxTracker_returns80cHeadroom() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        TaxTrackerResponse response = new TaxTrackerResponse(
                new BigDecimal("150000.00"),
                new BigDecimal("92500.00"),
                new BigDecimal("57500.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("35000.00"),
                new BigDecimal("17500.00"),
                BigDecimal.ZERO,
                new BigDecimal("12500.00"),
                new BigDecimal("3200.00"),
                "ITR-2",
                "Old Regime saves approx ₹18,400 with current 80C deductions");

        when(hubService.getTaxTracker(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/financial-hub/tax-tracker")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.section80cMaxLimit").value(150000.00))
                .andExpect(jsonPath("$.data.section80cClaimed").value(92500.00))
                .andExpect(jsonPath("$.data.section80cRemaining").value(57500.00));
    }
}
