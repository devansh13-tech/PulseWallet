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
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
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
}