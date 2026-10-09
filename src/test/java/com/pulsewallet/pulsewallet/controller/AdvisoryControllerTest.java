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
import com.pulsewallet.pulsewallet.dto.AdvisoryResponse;
import com.pulsewallet.pulsewallet.dto.MonthlyAdviceResponse;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.AdvisoryService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(AdvisoryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class AdvisoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvisoryService advisoryService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void monthlyAdviceUsesAuthenticatedPrincipalAndReturnsFullAdvice() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 5L));

        MonthlyAdviceResponse mockResponse = new MonthlyAdviceResponse(
                new BigDecimal("100000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("40.00"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("12000.00"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                "House",
                "ON_TRACK",
                BigDecimal.ZERO,
                "PRIORITY_6",
                "Your cash flow is positive and your emergency fund is ready. Continue your monthly investment plan and maintain diversification.",
                List.of("Continue your recommended monthly investment of ₹12000.00."),
                List.of());

        when(advisoryService.monthlyAdvice(5L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/advice/monthly").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").value(100000.00))
                .andExpect(jsonPath("$.data.totalExpenses").value(60000.00))
                .andExpect(jsonPath("$.data.disposableIncome").value(40000.00))
                .andExpect(jsonPath("$.data.savingsRate").value(40.00))
                .andExpect(jsonPath("$.data.emergencyFundReady").value(true))
                .andExpect(jsonPath("$.data.emergencyFundRemaining").value(0.00))
                .andExpect(jsonPath("$.data.recommendedMonthlyInvestment").value(12000.00))
                .andExpect(jsonPath("$.data.riskPreference").value("MODERATE"))
                .andExpect(jsonPath("$.data.diversified").value(true))
                .andExpect(jsonPath("$.data.diversificationStatus").value("DIVERSIFIED"))
                .andExpect(jsonPath("$.data.primaryGoalName").value("House"))
                .andExpect(jsonPath("$.data.primaryGoalStatus").value("ON_TRACK"))
                .andExpect(jsonPath("$.data.primaryGoalShortfall").value(0.00))
                .andExpect(jsonPath("$.data.advicePriority").value("PRIORITY_6"))
                .andExpect(jsonPath("$.data.advice").value("Your cash flow is positive and your emergency fund is ready. Continue your monthly investment plan and maintain diversification."))
                .andExpect(jsonPath("$.data.recommendations[0]").value("Continue your recommended monthly investment of ₹12000.00."));
    }

    @Test
    void monthlyAdviceRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/advice/monthly").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    void existingAdvisoryEndpointRemainsFunctional() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 5L));

        AdvisoryResponse mockAdvisory = new AdvisoryResponse(
                new BigDecimal("100000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("360000.00"),
                new BigDecimal("180000.00"),
                new BigDecimal("12000.00"),
                new BigDecimal("6000.00"),
                "illustrative guidance");

        when(advisoryService.advice(5L)).thenReturn(mockAdvisory);

        mockMvc.perform(get("/api/advisory").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.disposableIncome").value(40000.00));
    }
}
