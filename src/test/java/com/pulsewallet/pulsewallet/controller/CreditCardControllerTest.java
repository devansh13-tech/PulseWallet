package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.CreditCardIntelligenceService;
import com.pulsewallet.pulsewallet.service.CreditCardService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(CreditCardController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreditCardService creditCardService;
    @MockitoBean
    private CreditCardIntelligenceService intelligenceService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private UserPrincipal principal(Long userId) {
        return new UserPrincipal(
                TestEntities.withId(new User("Alice", "alice@example.com", "hash"), userId));
    }

    @Test
    void listUsesAuthenticatedPrincipal() throws Exception {
        UserPrincipal p = principal(7L);
        when(creditCardService.getAll(7L)).thenReturn(List.of(
                new CreditCardResponse(1L, "HDFC", "Regalia", "1234",
                        new BigDecimal("100000"), new BigDecimal("25000"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                        "Cashback", "Lounge", Instant.now(), Instant.now())));

        mockMvc.perform(get("/api/credit-cards").with(authentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].cardName").value("Regalia"))
                .andExpect(jsonPath("$.data[0].lastFourDigits").value("1234"));
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc.perform(get("/api/credit-cards").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void intelligenceEndpointReturnsData() throws Exception {
        UserPrincipal p = principal(7L);
        when(intelligenceService.getIntelligence(7L)).thenReturn(
                new CreditCardIntelligenceResponse(
                        List.of(), // no cards detail needed for this test
                        new CreditCardIntelligenceResponse.CreditCardSummary(
                                new BigDecimal("100000"), new BigDecimal("25000"),
                                new BigDecimal("75000"), new BigDecimal("25.00"),
                                1, 0, 0),
                        new CreditCardIntelligenceResponse.RepaymentCapacity(
                                "COMFORTABLE", new BigDecimal("40000"),
                                new BigDecimal("20000"), new BigDecimal("25000"),
                                List.of("Test explanation")),
                        List.of(), List.of()));

        mockMvc.perform(get("/api/credit-cards/intelligence").with(authentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalCreditLimit").value(100000))
                .andExpect(jsonPath("$.data.repaymentCapacity.status").value("COMFORTABLE"));
    }

    @Test
    void createRejectsInvalidRequest() throws Exception {
        UserPrincipal p = principal(7L);
        String invalidJson = """
                {
                  "issuer": "",
                  "cardName": "",
                  "lastFourDigits": "XY",
                  "creditLimit": -1,
                  "currentOutstanding": -5,
                  "statementDate": "2026-10-01",
                  "dueDate": "2026-10-15"
                }
                """;

        mockMvc.perform(post("/api/credit-cards")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAcceptsValidRequest() throws Exception {
        UserPrincipal p = principal(7L);
        String validJson = """
                {
                  "issuer": "HDFC",
                  "cardName": "Regalia",
                  "lastFourDigits": "1234",
                  "creditLimit": 100000,
                  "currentOutstanding": 25000,
                  "statementDate": "2026-10-01",
                  "dueDate": "2026-10-15"
                }
                """;
        when(creditCardService.create(eq(7L), any())).thenReturn(
                new CreditCardResponse(1L, "HDFC", "Regalia", "1234",
                        new BigDecimal("100000"), new BigDecimal("25000"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                        null, null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/credit-cards")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardName").value("Regalia"));
    }
}
