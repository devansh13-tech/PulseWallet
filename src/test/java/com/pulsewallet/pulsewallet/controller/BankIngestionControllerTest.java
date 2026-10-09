package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.BankIngestionDtos.*;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.BankIngestionService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(BankIngestionController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class BankIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankIngestionService bankIngestionService;

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
    void parseSms_returnsParsedTransaction() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        ParsedTransactionDto parsed = new ParsedTransactionDto(
                new BigDecimal("4500.00"),
                "EXPENSE",
                "ZOMATO UPI",
                "UPI",
                "**1294",
                new BigDecimal("28450.00"),
                "UPI429183921",
                "Food & Dining",
                LocalDate.of(2026, 10, 9),
                "HDFC Bank: Rs 4,500.00 debited");

        when(bankIngestionService.parseSms(any())).thenReturn(parsed);

        mockMvc.perform(post("/api/bank-ingestion/parse-sms")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "smsText": "HDFC Bank: Rs 4,500.00 debited from A/C **1294 on 09-OCT-26 at Zomato UPI"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchant").value("ZOMATO UPI"))
                .andExpect(jsonPath("$.data.amount").value(4500.00))
                .andExpect(jsonPath("$.data.suggestedCategory").value("Food & Dining"));
    }

    @Test
    void initiateAaConsent_returnsConsentApproval() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        AccountAggregatorConsentResponse consentResponse = new AccountAggregatorConsentResponse(
                "CONSENT-12345",
                "HDFC Bank",
                "CONSENT_APPROVED_ACTIVE",
                "Setu / Finvu AA (RBI Regulated)",
                List.of(),
                "2027-10-10T00:00:00Z");

        when(bankIngestionService.initiateAccountAggregator(any())).thenReturn(consentResponse);

        mockMvc.perform(post("/api/bank-ingestion/account-aggregator/consent")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "institutionId": "HDFC Bank",
                            "accountAggregatorVpa": "ada@okhdfcbank",
                            "mobileNumber": "9876543210"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consentId").value("CONSENT-12345"))
                .andExpect(jsonPath("$.data.status").value("CONSENT_APPROVED_ACTIVE"));
    }

    @Test
    void submitCategoryFeedback_recordsPreference() throws Exception {
        UserPrincipal principal = createPrincipal(1L);

        mockMvc.perform(post("/api/bank-ingestion/category-feedback")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "merchant": "SWIGGY",
                            "correctedCategory": "Food & Dining"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Categorizer updated with preference"));

        verify(bankIngestionService).recordCategoryFeedback("SWIGGY", "Food & Dining");
    }
}
