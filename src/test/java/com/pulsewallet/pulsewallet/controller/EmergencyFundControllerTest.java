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
import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse.EssentialExpenseCategory;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.EmergencyFundService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(EmergencyFundController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class EmergencyFundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmergencyFundService emergencyFundService;

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
    void getPlan_returnsEmergencyFundPlanWhenAuthenticated() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        EmergencyFundPlanResponse response = new EmergencyFundPlanResponse(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("20000.00"),
                6,
                new BigDecimal("120000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("25.00"),
                List.of(new EssentialExpenseCategory("Rent", new BigDecimal("15000.00")),
                        new EssentialExpenseCategory("Groceries", new BigDecimal("5000.00"))));

        when(emergencyFundService.calculate(eq(1L), any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/emergency-fund")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyEssentialExpenses").value(20000.00))
                .andExpect(jsonPath("$.data.recommendedMonths").value(6))
                .andExpect(jsonPath("$.data.recommendedEmergencyFundTarget").value(120000.00))
                .andExpect(jsonPath("$.data.currentEmergencyFund").value(30000.00))
                .andExpect(jsonPath("$.data.remainingAmountNeeded").value(90000.00))
                .andExpect(jsonPath("$.data.suggestedMonthlyContribution").value(2000.00))
                .andExpect(jsonPath("$.data.progressPercentage").value(25.00));
    }

    @Test
    void getPlan_requiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/emergency-fund").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPlan_passesQueryParamsToService() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        EmergencyFundPlanResponse response = new EmergencyFundPlanResponse(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("20000.00"),
                6,
                new BigDecimal("120000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("25.00"),
                List.of());

        when(emergencyFundService.calculate(
                1L,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("30000.00"),
                6)).thenReturn(response);

        mockMvc.perform(get("/api/emergency-fund")
                .param("from", "2026-08-01")
                .param("to", "2026-08-31")
                .param("currentAmount", "30000.00")
                .param("months", "6")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedMonths").value(6));
    }
}
