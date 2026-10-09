package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.FinancialSummaryService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(FinancialSummaryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class FinancialSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialSummaryService financialSummaryService;

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
    void summary_returnsSavingsRateInApiResponse() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        FinancialSummaryResponse response = new FinancialSummaryResponse(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("50000.00"),
                new BigDecimal("32000.00"),
                new BigDecimal("18000.00"),
                new BigDecimal("36.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        when(financialSummaryService.summarize(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/transactions/summary")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").value(50000.00))
                .andExpect(jsonPath("$.data.totalExpenses").value(32000.00))
                .andExpect(jsonPath("$.data.disposableIncome").value(18000.00))
                .andExpect(jsonPath("$.data.savingsRate").value(36.00));
    }

    @Test
    void summary_requiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/transactions/summary").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summary_passesDateFilterParameters() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        FinancialSummaryResponse response = new FinancialSummaryResponse(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("50000.00"),
                new BigDecimal("32000.00"),
                new BigDecimal("18000.00"),
                new BigDecimal("36.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        when(financialSummaryService.summarize(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(response);

        mockMvc.perform(get("/api/transactions/summary")
                .param("from", "2026-08-01")
                .param("to", "2026-08-31")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savingsRate").value(36.00));
    }
}
