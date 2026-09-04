package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.pulsewallet.pulsewallet.dto.NotificationResponse;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.NotificationService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void list_returnsNotificationsForAuthenticatedUser() throws Exception {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        UserPrincipal principal = new UserPrincipal(user);
        NotificationResponse response = new NotificationResponse(
                7L,
                "Suspicious transaction detected",
                "A potentially fraudulent transaction of ₹149.62 was detected.",
                "FRAUD_ALERT",
                false,
                Instant.parse("2026-08-29T10:00:00Z"),
                null);
        when(notificationService.list(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications")
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Suspicious transaction detected"));
    }

    @Test
    void list_requiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/notifications").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markRead_rejectsCrossUserAccess() throws Exception {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        UserPrincipal principal = new UserPrincipal(user);
        when(notificationService.markRead(8L, 1L)).thenThrow(new ResourceNotFoundException("Notification", 8L));

        mockMvc.perform(patch("/api/notifications/8/read")
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isNotFound());
    }
}
