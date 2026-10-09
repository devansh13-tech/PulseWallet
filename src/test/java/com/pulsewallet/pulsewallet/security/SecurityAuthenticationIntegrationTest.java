package com.pulsewallet.pulsewallet.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.controller.TransactionController;
import com.pulsewallet.pulsewallet.dto.TransactionRequest;
import com.pulsewallet.pulsewallet.dto.TransactionResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.service.TransactionService;

import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

/**
 * Milestone 7 deliverable: Auth tests covering:
 * - Unauthenticated requests
 * - Malformed JWT tokens
 * - Expired JWT tokens
 * - Cross-user access isolation (data scoped to caller only, preventing IDOR attacks)
 */
@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@AutoConfigureMockMvc(addFilters = true)
class SecurityAuthenticationIntegrationTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String EXPIRED_TOKEN = "expired.jwt.token";
    private static final String MALFORMED_TOKEN = "malformed.garbage.token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private UserPrincipal validPrincipal;

    @BeforeEach
    void setUp() {
        User user = new User("Ada", "ada@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
        validPrincipal = new UserPrincipal(user);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns401Unauthorized() throws Exception {
        when(jwtService.extractUsername(MALFORMED_TOKEN))
                .thenThrow(new MalformedJwtException("Malformed JWT string"));

        mockMvc.perform(get("/api/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + MALFORMED_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401Unauthorized() throws Exception {
        when(jwtService.extractUsername(EXPIRED_TOKEN))
                .thenThrow(new ExpiredJwtException(null, null, "JWT token has expired"));

        mockMvc.perform(get("/api/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + EXPIRED_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_returns200Ok() throws Exception {
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("ada@example.com");
        when(customUserDetailsService.loadUserByUsername("ada@example.com")).thenReturn(validPrincipal);
        when(jwtService.isValid(eq(VALID_TOKEN), any())).thenReturn(true);
        when(transactionService.list(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void crossUserAccess_readingAnotherUsersTransaction_returns404NotFound() throws Exception {
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("ada@example.com");
        when(customUserDetailsService.loadUserByUsername("ada@example.com")).thenReturn(validPrincipal);
        when(jwtService.isValid(eq(VALID_TOKEN), any())).thenReturn(true);

        Long otherUsersTransactionId = 999L;
        when(transactionService.get(eq(otherUsersTransactionId), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Transaction", otherUsersTransactionId));

        mockMvc.perform(get("/api/transactions/{id}", otherUsersTransactionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Transaction not found with id 999"));
    }

    @Test
    void crossUserAccess_updatingAnotherUsersTransaction_returns404NotFound() throws Exception {
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("ada@example.com");
        when(customUserDetailsService.loadUserByUsername("ada@example.com")).thenReturn(validPrincipal);
        when(jwtService.isValid(eq(VALID_TOKEN), any())).thenReturn(true);

        Long otherUsersTransactionId = 888L;
        when(transactionService.update(eq(otherUsersTransactionId), eq(1L), any(TransactionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Transaction", otherUsersTransactionId));

        mockMvc.perform(put("/api/transactions/{id}", otherUsersTransactionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 100.00,
                            "description": "Unauthorized attempt",
                            "type": "EXPENSE",
                            "transactionDate": "2026-08-10"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crossUserAccess_deletingAnotherUsersTransaction_returns404NotFound() throws Exception {
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn("ada@example.com");
        when(customUserDetailsService.loadUserByUsername("ada@example.com")).thenReturn(validPrincipal);
        when(jwtService.isValid(eq(VALID_TOKEN), any())).thenReturn(true);

        Long otherUsersTransactionId = 777L;
        doThrow(new ResourceNotFoundException("Transaction", otherUsersTransactionId))
                .when(transactionService).delete(eq(otherUsersTransactionId), eq(1L));

        mockMvc.perform(delete("/api/transactions/{id}", otherUsersTransactionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
