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
import java.time.Instant;
import java.time.LocalDate;
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
import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.dto.DashboardSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FraudSummaryResponse;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.DashboardSummaryService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(DashboardSummaryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class DashboardSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardSummaryService dashboardSummaryService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void dashboardSummary_returnsAuthenticatedUsersDashboard() throws Exception {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        UserPrincipal principal = new UserPrincipal(user);

        DashboardSummaryResponse response = new DashboardSummaryResponse(
                new FinancialSummaryResponse(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        new BigDecimal("3400.00"),
                        new BigDecimal("2100.00"),
                        new BigDecimal("1300.00"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()),
                List.of(new BudgetResponse(
                        11L,
                        new BigDecimal("1200.00"),
                        5L,
                        "Housing",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"))),
                new FraudSummaryResponse(2L, 1L, List.of()));

        when(dashboardSummaryService.getSummary(1L)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard-summary")
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.financialSummary.totalIncome").value(3400.00))
                .andExpect(jsonPath("$.data.fraudSummary.totalAlerts").value(2));
    }

    @Test
    void dashboardSummary_requiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/dashboard-summary").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}
