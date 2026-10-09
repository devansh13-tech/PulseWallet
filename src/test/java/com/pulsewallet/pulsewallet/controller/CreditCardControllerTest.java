package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsewallet.pulsewallet.config.SecurityConfig;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.GlobalExceptionHandler;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.CreditCardIntelligenceService;
import com.pulsewallet.pulsewallet.service.CreditCardService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(CreditCardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
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
    @MockitoBean
    private com.pulsewallet.pulsewallet.service.cardid.CardIdentificationService cardIdentificationService;
    @MockitoBean
    private com.pulsewallet.pulsewallet.service.statement.StatementPdfParserService statementPdfParserService;
    @MockitoBean
    private com.pulsewallet.pulsewallet.service.bankdata.BankDataIntegrationProvider bankDataIntegrationProvider;

    private UserPrincipal principal(Long userId) {
        return new UserPrincipal(
                TestEntities.withId(new User("Alice", "alice@example.com", "hash"), userId));
    }

    private void mockUnauthorized() throws Exception {
        doAnswer(invocation -> {
            var response = invocation.getArgument(1, jakarta.servlet.http.HttpServletResponse.class);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }).when(restAuthenticationEntryPoint).commence(any(), any(), any());
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
                .andExpect(jsonPath("$.data[0].lastFourDigits").value("1234"))
                .andExpect(jsonPath("$.data[0].availableCredit").value(75000.00))
                .andExpect(jsonPath("$.data[0].creditUtilization").value(25.00));
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockUnauthorized();

        mockMvc.perform(get("/api/credit-cards").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getByIdReturnsCardForAuthenticatedUser() throws Exception {
        UserPrincipal p = principal(7L);
        when(creditCardService.getById(1L, 7L)).thenReturn(
                new CreditCardResponse(1L, "HDFC", "Regalia", "1234",
                        new BigDecimal("100000"), new BigDecimal("25000"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                        "Cashback", "Lounge", Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/credit-cards/1").with(authentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.cardName").value("Regalia"))
                .andExpect(jsonPath("$.data.availableCredit").value(75000.00))
                .andExpect(jsonPath("$.data.creditUtilization").value(25.00));
    }

    @Test
    void getByIdReturns404WhenCardNotFoundOrNotOwned() throws Exception {
        UserPrincipal p = principal(7L);
        when(creditCardService.getById(99L, 7L)).thenThrow(new ResourceNotFoundException("Credit card", 99L));

        mockMvc.perform(get("/api/credit-cards/99").with(authentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getByIdRequiresAuthentication() throws Exception {
        mockUnauthorized();

        mockMvc.perform(get("/api/credit-cards/1").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void intelligenceEndpointReturnsData() throws Exception {
        UserPrincipal p = principal(7L);
        when(intelligenceService.getIntelligence(7L)).thenReturn(
                new CreditCardIntelligenceResponse(
                        List.of(),
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
                  "cardNetwork": "VISA",
                  "lastFourDigits": "1234",
                  "creditLimit": 100000,
                  "currentOutstanding": 25000,
                  "statementDate": "2026-10-01",
                  "dueDate": "2026-10-15",
                  "annualFee": 1000
                }
                """;
        when(creditCardService.create(eq(7L), any())).thenReturn(
                new CreditCardResponse(1L, "HDFC", "Regalia", "VISA", "1234",
                        new BigDecimal("100000"), new BigDecimal("25000"),
                        new BigDecimal("75000.00"), new BigDecimal("25.00"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                        new BigDecimal("1000.00"), null, null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/credit-cards")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardName").value("Regalia"))
                .andExpect(jsonPath("$.data.cardNetwork").value("VISA"))
                .andExpect(jsonPath("$.data.availableCredit").value(75000.00))
                .andExpect(jsonPath("$.data.creditUtilization").value(25.00));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockUnauthorized();

        mockMvc.perform(post("/api/credit-cards")
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateModifiesCardForAuthenticatedUser() throws Exception {
        UserPrincipal p = principal(7L);
        String updateJson = """
                {
                  "issuer": "ICICI",
                  "cardName": "Amazon Pay",
                  "cardNetwork": "VISA",
                  "lastFourDigits": "5678",
                  "creditLimit": 200000,
                  "currentOutstanding": 10000,
                  "statementDate": "2026-11-01",
                  "dueDate": "2026-11-20",
                  "annualFee": 0
                }
                """;

        when(creditCardService.update(eq(1L), eq(7L), any())).thenReturn(
                new CreditCardResponse(1L, "ICICI", "Amazon Pay", "VISA", "5678",
                        new BigDecimal("200000"), new BigDecimal("10000"),
                        new BigDecimal("190000.00"), new BigDecimal("5.00"),
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20),
                        BigDecimal.ZERO, null, null, Instant.now(), Instant.now()));

        mockMvc.perform(put("/api/credit-cards/1")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardName").value("Amazon Pay"))
                .andExpect(jsonPath("$.data.availableCredit").value(190000.00))
                .andExpect(jsonPath("$.data.creditUtilization").value(5.00));
    }

    @Test
    void updateReturns404WhenCardNotFoundOrNotOwned() throws Exception {
        UserPrincipal p = principal(7L);
        String updateJson = """
                {
                  "issuer": "ICICI",
                  "cardName": "Amazon Pay",
                  "lastFourDigits": "5678",
                  "creditLimit": 200000,
                  "currentOutstanding": 10000,
                  "statementDate": "2026-11-01",
                  "dueDate": "2026-11-20"
                }
                """;

        when(creditCardService.update(eq(99L), eq(7L), any()))
                .thenThrow(new ResourceNotFoundException("Credit card", 99L));

        mockMvc.perform(put("/api/credit-cards/99")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateRequiresAuthentication() throws Exception {
        mockUnauthorized();

        mockMvc.perform(put("/api/credit-cards/1")
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRemovesCardForAuthenticatedUser() throws Exception {
        UserPrincipal p = principal(7L);

        mockMvc.perform(delete("/api/credit-cards/1").with(authentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Credit card deleted"));

        verify(creditCardService).delete(1L, 7L);
    }

    @Test
    void deleteReturns404WhenCardNotFoundOrNotOwned() throws Exception {
        UserPrincipal p = principal(7L);
        doThrow(new ResourceNotFoundException("Credit card", 99L))
                .when(creditCardService).delete(99L, 7L);

        mockMvc.perform(delete("/api/credit-cards/99").with(authentication(
                new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteRequiresAuthentication() throws Exception {
        mockUnauthorized();

        mockMvc.perform(delete("/api/credit-cards/1").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void identifyCardReturnsSuccess() throws Exception {
        UserPrincipal p = principal(7L);
        when(cardIdentificationService.identify(any())).thenReturn(
                new com.pulsewallet.pulsewallet.dto.CardIdentificationResponse(
                        "HDFC Bank", "VISA", "Regalia Gold", "CONFIRMED", "LOCAL_BIN_CATALOGUE",
                        false, "•••• •••• •••• 0058", "411122", "Identified via local catalogue"));

        String reqJson = """
                {
                  "cardNumber": "4111220000000058"
                }
                """;

        mockMvc.perform(post("/api/credit-cards/identify")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issuer").value("HDFC Bank"))
                .andExpect(jsonPath("$.data.cardNetwork").value("VISA"))
                .andExpect(jsonPath("$.data.maskedCardNumber").value("•••• •••• •••• 0058"));
    }

    @Test
    void parseStatementPdfUploadsAndReturnsDraft() throws Exception {
        UserPrincipal p = principal(7L);
        when(statementPdfParserService.parseStatementPdf(any())).thenReturn(
                new com.pulsewallet.pulsewallet.dto.StatementExtractionResponse(
                        "statement.pdf", "HDFC Bank", "Regalia", "VISA", "1234",
                        new BigDecimal("100000"), new BigDecimal("25000"), new BigDecimal("1250"),
                        new BigDecimal("75000"), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                        "Sep 2026", "1500", false, false, true, "Parsed successfully", List.of()));

        org.springframework.mock.web.MockMultipartFile mockFile =
                new org.springframework.mock.web.MockMultipartFile("file", "statement.pdf", "application/pdf", "%PDF-1.4 test".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/credit-cards/statements/parse")
                .file(mockFile)
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issuer").value("HDFC Bank"))
                .andExpect(jsonPath("$.data.requiresUserConfirmation").value(true));
    }

    @Test
    void confirmStatementSavesCard() throws Exception {
        UserPrincipal p = principal(7L);
        when(statementPdfParserService.confirmAndSave(eq(7L), any())).thenReturn(
                new CreditCardResponse(1L, "HDFC Bank", "Regalia", "VISA", "1234",
                        new BigDecimal("100000"), new BigDecimal("25000"),
                        new BigDecimal("75000.00"), new BigDecimal("25.00"),
                        new BigDecimal("25000"), new BigDecimal("1250"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                        BigDecimal.ZERO, "STATEMENT_UPLOAD", "FRESH", Instant.now(), null, null, Instant.now(), Instant.now()));

        String confirmJson = """
                {
                  "issuer": "HDFC Bank",
                  "cardName": "Regalia",
                  "cardNetwork": "VISA",
                  "lastFourDigits": "1234",
                  "creditLimit": 100000,
                  "currentOutstanding": 25000,
                  "statementBalance": 25000,
                  "minimumDue": 1250,
                  "statementDate": "2026-10-01",
                  "dueDate": "2026-10-15"
                }
                """;

        mockMvc.perform(post("/api/credit-cards/statements/confirm")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dataSource").value("STATEMENT_UPLOAD"));
    }

    @Test
    void getBenefitsReturnsCardBenefits() throws Exception {
        UserPrincipal p = principal(7L);
        when(intelligenceService.getCardBenefits(1L, 7L)).thenReturn(
                new com.pulsewallet.pulsewallet.dto.CardBenefitsResponse(
                        1L, "HDFC Bank", "Regalia", new BigDecimal("2500.00"),
                        new BigDecimal("1.33"), java.util.Map.of("TRAVEL", new BigDecimal("4.00")),
                        List.of("FUEL", "RENT"), List.of("Lounge access"), "PUBLISHED_BENEFIT_CATALOGUE", true));

        mockMvc.perform(get("/api/credit-cards/1/benefits")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardName").value("Regalia"))
                .andExpect(jsonPath("$.data.publishedAnnualFee").value(2500.00));
    }

    @Test
    void getBankSyncStatusReturnsStatus() throws Exception {
        UserPrincipal p = principal(7L);
        when(bankDataIntegrationProvider.getIntegrationStatus()).thenReturn(
                new com.pulsewallet.pulsewallet.dto.BankSyncStatusResponse(
                        "RBI AA", false, "PENDING_REGISTRATION",
                        List.of("HDFC Bank"), List.of("Credit Limit"), List.of("OAuth2"), List.of("FIU"), "Readiness mode"));

        mockMvc.perform(get("/api/credit-cards/bank-sync/status")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.frameworkName").value("RBI AA"));
    }

    @Test
    void initiateBankConsentReturnsConsentHandle() throws Exception {
        UserPrincipal p = principal(7L);
        when(bankDataIntegrationProvider.initiateConsent(eq(7L), any())).thenReturn(
                new com.pulsewallet.pulsewallet.dto.BankConsentResponse(
                        "handle-123", "hdfc-bank", "AWAITING_USER_APPROVAL",
                        "https://aa.example.com/consent/handle-123", List.of("STATEMENT_READ"), "Disclaimer"));

        String consentJson = """
                {
                  "institutionId": "hdfc-bank",
                  "consentPurpose": "Smart Credit Card Management"
                }
                """;

        mockMvc.perform(post("/api/credit-cards/bank-sync/consent")
                .with(authentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consentHandle").value("handle-123"));
    }
}
