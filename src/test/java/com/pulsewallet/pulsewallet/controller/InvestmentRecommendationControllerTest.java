package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.InvestmentProjectionResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.InvestmentRecommendationService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(InvestmentRecommendationController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class InvestmentRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvestmentRecommendationService investmentRecommendationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void getRecommendationUsesAuthenticatedPrincipalAndReturnsFullPayload() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));

        InvestmentRecommendationResponse mockResponse = new InvestmentRecommendationResponse(
                new BigDecimal("6000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("30.00"),
                true,
                new BigDecimal("0.00"),
                InvestmentRiskPreference.MODERATE,
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                new BigDecimal("10.00"),
                new BigDecimal("3600.00"),
                new BigDecimal("1800.00"),
                new BigDecimal("600.00"),
                true,
                "DIVERSIFIED",
                "Diversified allocation across Equity, Debt, and Gold.",
                "House",
                new BigDecimal("2000000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("1800000.00"),
                new BigDecimal("5000.00"),
                true,
                "Sufficient investment capacity",
                List.of());

        when(investmentRecommendationService.recommend(7L, null)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/investments/recommendation").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendedMonthlyInvestment").value(6000.00))
                .andExpect(jsonPath("$.data.disposableIncome").value(20000.00))
                .andExpect(jsonPath("$.data.investmentAllocationPercentage").value(30.00))
                .andExpect(jsonPath("$.data.emergencyFundReady").value(true))
                .andExpect(jsonPath("$.data.emergencyFundRemaining").value(0.00))
                .andExpect(jsonPath("$.data.riskPreference").value("MODERATE"))
                .andExpect(jsonPath("$.data.equityPercentage").value(60.00))
                .andExpect(jsonPath("$.data.debtPercentage").value(30.00))
                .andExpect(jsonPath("$.data.goldPercentage").value(10.00))
                .andExpect(jsonPath("$.data.equityAmount").value(3600.00))
                .andExpect(jsonPath("$.data.debtAmount").value(1800.00))
                .andExpect(jsonPath("$.data.goldAmount").value(600.00))
                .andExpect(jsonPath("$.data.diversified").value(true))
                .andExpect(jsonPath("$.data.diversificationStatus").value("DIVERSIFIED"))
                .andExpect(jsonPath("$.data.diversificationMessage").isNotEmpty())
                .andExpect(jsonPath("$.data.goalName").value("House"))
                .andExpect(jsonPath("$.data.goalContributionCovered").value(true));
    }

    @Test
    void getRecommendationPassesGoalIdWhenProvided() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));

        InvestmentRecommendationResponse mockResponse = new InvestmentRecommendationResponse(
                new BigDecimal("6000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("30.00"),
                true,
                new BigDecimal("0.00"),
                InvestmentRiskPreference.MODERATE,
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                new BigDecimal("10.00"),
                new BigDecimal("3600.00"),
                new BigDecimal("1800.00"),
                new BigDecimal("600.00"),
                true,
                "DIVERSIFIED",
                "Diversified allocation across Equity, Debt, and Gold.",
                "Car",
                new BigDecimal("500000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("400000.00"),
                new BigDecimal("8000.00"),
                false,
                "Goal contribution exceeds capacity",
                List.of("Goal shortfall"));

        when(investmentRecommendationService.recommend(7L, 15L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/investments/recommendation?goalId=15").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.goalName").value("Car"))
                .andExpect(jsonPath("$.data.goalContributionCovered").value(false))
                .andExpect(jsonPath("$.data.diversified").value(true))
                .andExpect(jsonPath("$.data.diversificationStatus").value("DIVERSIFIED"));
    }

    @Test
    void getRecommendationRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/investments/recommendation").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProjectionUsesAuthenticatedPrincipalAndReturnsFullPayload() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));

        InvestmentProjectionResponse mockProjection = new InvestmentProjectionResponse(
                new BigDecimal("6000.00"),
                10,
                120,
                new BigDecimal("10.00"),
                new BigDecimal("0.8333"),
                new BigDecimal("720000.00"),
                new BigDecimal("519312.28"),
                new BigDecimal("1239312.28"),
                InvestmentRiskPreference.MODERATE,
                "House",
                new BigDecimal("2000000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("1800000.00"),
                false,
                "Projected growth message",
                List.of());

        when(investmentRecommendationService.project(7L, null, 10)).thenReturn(mockProjection);

        mockMvc.perform(get("/api/investments/projection").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyInvestment").value(6000.00))
                .andExpect(jsonPath("$.data.years").value(10))
                .andExpect(jsonPath("$.data.months").value(120))
                .andExpect(jsonPath("$.data.annualExpectedReturnPercentage").value(10.00))
                .andExpect(jsonPath("$.data.monthlyExpectedReturnPercentage").value(0.8333))
                .andExpect(jsonPath("$.data.totalContributed").value(720000.00))
                .andExpect(jsonPath("$.data.estimatedGrowth").value(519312.28))
                .andExpect(jsonPath("$.data.projectedFutureValue").value(1239312.28))
                .andExpect(jsonPath("$.data.riskPreference").value("MODERATE"))
                .andExpect(jsonPath("$.data.goalName").value("House"))
                .andExpect(jsonPath("$.data.goalProjectedValueCovered").value(false));
    }

    @Test
    void getProjectionPassesGoalIdAndYearsWhenProvided() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));

        InvestmentProjectionResponse mockProjection = new InvestmentProjectionResponse(
                new BigDecimal("6000.00"),
                15,
                180,
                new BigDecimal("10.00"),
                new BigDecimal("0.8333"),
                new BigDecimal("1080000.00"),
                new BigDecimal("1406834.72"),
                new BigDecimal("2486834.72"),
                InvestmentRiskPreference.MODERATE,
                "House",
                new BigDecimal("2000000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("1800000.00"),
                true,
                "Projected growth message covers goal",
                List.of());

        when(investmentRecommendationService.project(7L, 5L, 15)).thenReturn(mockProjection);

        mockMvc.perform(get("/api/investments/projection?goalId=5&years=15").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.years").value(15))
                .andExpect(jsonPath("$.data.months").value(180))
                .andExpect(jsonPath("$.data.goalProjectedValueCovered").value(true));
    }

    @Test
    void getProjectionRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/investments/projection").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}
