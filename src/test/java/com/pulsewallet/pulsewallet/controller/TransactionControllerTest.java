package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.TransactionRequest;
import com.pulsewallet.pulsewallet.dto.TransactionResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.TransactionService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

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
    void create_createsTransactionWithAutoCategorization() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        TransactionResponse response = new TransactionResponse(
                10L,
                new BigDecimal("450.00"),
                "Swiggy dinner",
                null,
                null,
                2L,
                "Food & Dining",
                TransactionType.EXPENSE,
                LocalDate.of(2026, 8, 10),
                Instant.now(),
                Instant.now());

        when(transactionService.create(eq(1L), any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 450.00,
                            "description": "Swiggy dinner",
                            "type": "EXPENSE",
                            "transactionDate": "2026-08-10"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.categoryName").value("Food & Dining"))
                .andExpect(jsonPath("$.data.description").value("Swiggy dinner"));
    }

    @Test
    void create_createsIncomeTransactionWithAutoCategorization() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        TransactionResponse response = new TransactionResponse(
                11L,
                new BigDecimal("75000.00"),
                "Salary credited",
                null,
                null,
                1L,
                "Salary",
                TransactionType.INCOME,
                LocalDate.of(2026, 8, 1),
                Instant.now(),
                Instant.now());

        when(transactionService.create(eq(1L), any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 75000.00,
                            "description": "Salary credited",
                            "type": "INCOME",
                            "transactionDate": "2026-08-01"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.categoryName").value("Salary"))
                .andExpect(jsonPath("$.data.type").value("INCOME"));
    }

    @Test
    void create_unauthenticatedReturnsUnauthorized() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(post("/api/transactions")
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 100.00,
                            "description": "Coffee",
                            "type": "EXPENSE",
                            "transactionDate": "2026-08-10"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_returnsSingleTransaction() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        TransactionResponse response = new TransactionResponse(
                15L,
                new BigDecimal("1200.00"),
                "Uber ride",
                null,
                null,
                5L,
                "Transportation",
                TransactionType.EXPENSE,
                LocalDate.of(2026, 8, 10),
                Instant.now(),
                Instant.now());

        when(transactionService.get(15L, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/transactions/15")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(15))
                .andExpect(jsonPath("$.data.categoryName").value("Transportation"));
    }

    @Test
    void list_returnsPageOfTransactions() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        TransactionResponse response = new TransactionResponse(
                15L,
                new BigDecimal("1200.00"),
                "Uber ride",
                null,
                null,
                5L,
                "Transportation",
                TransactionType.EXPENSE,
                LocalDate.of(2026, 8, 10),
                Instant.now(),
                Instant.now());

        when(transactionService.list(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/transactions")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(15));
    }

    @Test
    void update_updatesTransaction() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        TransactionResponse response = new TransactionResponse(
                20L,
                new BigDecimal("500.00"),
                "Electricity bill",
                null,
                null,
                4L,
                "Utilities",
                TransactionType.EXPENSE,
                LocalDate.of(2026, 8, 10),
                Instant.now(),
                Instant.now());

        when(transactionService.update(eq(20L), eq(1L), any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/transactions/20")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 500.00,
                            "description": "Electricity bill",
                            "type": "EXPENSE",
                            "transactionDate": "2026-08-10"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryName").value("Utilities"));
    }

    @Test
    void delete_deletesTransaction() throws Exception {
        UserPrincipal principal = createPrincipal(1L);

        mockMvc.perform(delete("/api/transactions/25")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction deleted"));

        verify(transactionService).delete(25L, 1L);
    }
}
