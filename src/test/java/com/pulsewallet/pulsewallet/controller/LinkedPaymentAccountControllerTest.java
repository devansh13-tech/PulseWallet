package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.pulsewallet.pulsewallet.dto.LinkedPaymentAccountResponse;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.LinkedPaymentAccountService;
import com.pulsewallet.pulsewallet.support.TestEntities;
import com.pulsewallet.pulsewallet.entity.User;

@WebMvcTest(LinkedPaymentAccountController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class LinkedPaymentAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private LinkedPaymentAccountService accountService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private final LinkedPaymentAccountResponse account = new LinkedPaymentAccountResponse(1L, "Demo Bank",
            "My Savings Account", "BANK", "CONNECTED", "****1234", Instant.parse("2026-01-01T00:00:00Z"), null);

    @Test
    void createLinkedAccount() throws Exception {
        when(accountService.create(any(Long.class), any())).thenReturn(account);

        mockMvc.perform(post("/api/linked-accounts").with(authenticated(7L))
                .contentType("application/json")
                .content("{\"providerName\":\"Demo Bank\",\"accountName\":\"My Savings Account\",\"accountType\":\"BANK\",\"maskedIdentifier\":\"****1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.providerName").value("Demo Bank"));
    }

    @Test
    void listGetsOnlyAuthenticatedUsersAccounts() throws Exception {
        when(accountService.list(7L)).thenReturn(List.of(account));

        mockMvc.perform(get("/api/linked-accounts").with(authenticated(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].maskedIdentifier").value("****1234"));
    }

    @Test
    void getSingleAccount() throws Exception {
        when(accountService.get(1L, 7L)).thenReturn(account);

        mockMvc.perform(get("/api/linked-accounts/1").with(authenticated(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void disconnectAccount() throws Exception {
        LinkedPaymentAccountResponse disconnected = new LinkedPaymentAccountResponse(1L, "Demo Bank",
                "My Savings Account", "BANK", "DISCONNECTED", "****1234", account.connectedAt(), null);
        when(accountService.disconnect(1L, 7L)).thenReturn(disconnected);

        mockMvc.perform(patch("/api/linked-accounts/1/disconnect").with(authenticated(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISCONNECTED"));
    }

    @Test
    void updateBalance() throws Exception {
        LinkedPaymentAccountResponse updated = new LinkedPaymentAccountResponse(1L, "Demo Bank",
                "My Savings Account", "BANK", "CONNECTED", "****1234", java.math.BigDecimal.valueOf(50000.00), "Savings", "HDFC0001234", account.connectedAt(), null);
        when(accountService.updateBalance(any(Long.class), any(Long.class), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/linked-accounts/1/balance").with(authenticated(7L))
                .contentType("application/json")
                .content("{\"balance\":50000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(50000.00));
    }

    @Test
    void deleteAccount() throws Exception {
        mockMvc.perform(delete("/api/linked-accounts/1").with(authenticated(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void validationRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/linked-accounts").with(authenticated(7L))
                .contentType("application/json").content("{\"providerName\":\"Demo Bank\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticationIsRequired() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/linked-accounts").with(anonymous())).andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(Long userId) {
        UserPrincipal principal = new UserPrincipal(
                TestEntities.withId(new User("Ada", "ada" + userId + "@example.com", "hash"), userId));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
