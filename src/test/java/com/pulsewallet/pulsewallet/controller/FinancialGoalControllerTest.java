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
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.GoalAchievementResponse;
import com.pulsewallet.pulsewallet.dto.GoalPlanResponse;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.FinancialGoalService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(FinancialGoalController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class FinancialGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FinancialGoalService goalService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void listUsesAuthenticatedPrincipal() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));
        when(goalService.list(7L)).thenReturn(List.of(new FinancialGoalResponse(1L, "Bike", "BIKE",
                new BigDecimal("200000"), new BigDecimal("40000"), new BigDecimal("160000"), new BigDecimal("20"),
                10, new BigDecimal("16000"), "ON_TRACK", null, null, null)));

        mockMvc.perform(get("/api/goals").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Bike"));
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/goals").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    void getPlanUsesAuthenticatedPrincipalAndReturnsFullReversePlan() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));
        GoalPlanResponse mockPlan = new GoalPlanResponse(
                1L,
                "House Downpayment",
                "HOUSE",
                new BigDecimal("2000000.00"),
                new BigDecimal("200000.00"),
                LocalDate.of(2034, 9, 4),
                new BigDecimal("10.00"),
                new BigDecimal("1800000.00"),
                96,
                new BigDecimal("18750.00"),
                new BigDecimal("25000.00"),
                true,
                new BigDecimal("0.00"),
                "IN_PROGRESS",
                "Saving for 20% downpayment");

        when(goalService.getPlan(1L, 7L)).thenReturn(mockPlan);

        mockMvc.perform(get("/api/goals/1/plan").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.goalId").value(1))
                .andExpect(jsonPath("$.data.goalName").value("House Downpayment"))
                .andExpect(jsonPath("$.data.goalType").value("HOUSE"))
                .andExpect(jsonPath("$.data.targetAmount").value(2000000.00))
                .andExpect(jsonPath("$.data.currentAmount").value(200000.00))
                .andExpect(jsonPath("$.data.targetDate").value("2034-09-04"))
                .andExpect(jsonPath("$.data.progressPercentage").value(10.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(1800000.00))
                .andExpect(jsonPath("$.data.remainingMonths").value(96))
                .andExpect(jsonPath("$.data.requiredMonthlyContribution").value(18750.00))
                .andExpect(jsonPath("$.data.disposableIncome").value(25000.00))
                .andExpect(jsonPath("$.data.affordable").value(true))
                .andExpect(jsonPath("$.data.shortfallAmount").value(0.00))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.description").value("Saving for 20% downpayment"));
    }

    @Test
    void getPlanRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/goals/1/plan").with(anonymous())).andExpect(status().isUnauthorized());
    }

    @Test
    void getAchievementUsesAuthenticatedPrincipalAndReturnsAnalysis() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 7L));
        GoalAchievementResponse mockAchievement = new GoalAchievementResponse(
                1L,
                "House Downpayment",
                new BigDecimal("2000000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("1800000.00"),
                LocalDate.of(2034, 9, 4),
                new BigDecimal("10.00"),
                96,
                new BigDecimal("18750.00"),
                new BigDecimal("25000.00"),
                new BigDecimal("7500.00"),
                new BigDecimal("11250.00"),
                new BigDecimal("0.00"),
                new BigDecimal("1650000.00"),
                "AT_RISK",
                false,
                "Goal 'House Downpayment' is at risk.",
                List.of("Monthly shortfall of ₹11250.00"));

        when(goalService.getAchievement(1L, 7L)).thenReturn(mockAchievement);

        mockMvc.perform(get("/api/goals/1/achievement").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.goalId").value(1))
                .andExpect(jsonPath("$.data.goalName").value("House Downpayment"))
                .andExpect(jsonPath("$.data.targetAmount").value(2000000.00))
                .andExpect(jsonPath("$.data.currentAmount").value(200000.00))
                .andExpect(jsonPath("$.data.remainingAmount").value(1800000.00))
                .andExpect(jsonPath("$.data.targetDate").value("2034-09-04"))
                .andExpect(jsonPath("$.data.progressPercentage").value(10.00))
                .andExpect(jsonPath("$.data.remainingMonths").value(96))
                .andExpect(jsonPath("$.data.requiredMonthlyContribution").value(18750.00))
                .andExpect(jsonPath("$.data.disposableIncome").value(25000.00))
                .andExpect(jsonPath("$.data.recommendedMonthlyInvestment").value(7500.00))
                .andExpect(jsonPath("$.data.monthlyShortfall").value(11250.00))
                .andExpect(jsonPath("$.data.monthlySurplus").value(0.00))
                .andExpect(jsonPath("$.data.projectedAmountAtTarget").value(1650000.00))
                .andExpect(jsonPath("$.data.achievementStatus").value("AT_RISK"))
                .andExpect(jsonPath("$.data.achievable").value(false))
                .andExpect(jsonPath("$.data.analysisMessage").value("Goal 'House Downpayment' is at risk."))
                .andExpect(jsonPath("$.data.warnings[0]").value("Monthly shortfall of ₹11250.00"));
    }

    @Test
    void getAchievementRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/goals/1/achievement").with(anonymous())).andExpect(status().isUnauthorized());
    }
}