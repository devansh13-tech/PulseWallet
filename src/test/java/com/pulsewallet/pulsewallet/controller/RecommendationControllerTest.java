package com.pulsewallet.pulsewallet.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
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
import com.pulsewallet.pulsewallet.dto.RecommendationResponse;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.service.RecommendationService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(RecommendationController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void recommendationsUsesAuthenticatedUserId() throws Exception {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 17L);
        UserPrincipal principal = new UserPrincipal(user);
        when(recommendationService.recommend(17L)).thenReturn(new RecommendationResponse(
                new BigDecimal("80000"), new BigDecimal("35000"), new BigDecimal("45000"),
                new BigDecimal("35000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("35000"),
                new BigDecimal("10000"), List.of(), "deterministic-rules-v1"));

        mockMvc.perform(get("/api/recommendations").with(authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyLeft").value(45000.00));
    }

    @Test
    void recommendationsRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/recommendations").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}