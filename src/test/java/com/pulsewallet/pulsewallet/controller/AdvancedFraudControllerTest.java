package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.pulsewallet.pulsewallet.dto.FraudHubDtos.*;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.security.CustomUserDetailsService;
import com.pulsewallet.pulsewallet.security.JwtService;
import com.pulsewallet.pulsewallet.security.RestAccessDeniedHandler;
import com.pulsewallet.pulsewallet.security.RestAuthenticationEntryPoint;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.AdvancedFraudService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@WebMvcTest(AdvancedFraudController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class AdvancedFraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvancedFraudService fraudService;

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
    void getSecurityCenter_returnsActiveAlertsAndStatus() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        SecurityCenterResponse response = new SecurityCenterResponse(
                "DEFCON 2 - ELEVATED VIGILANCE",
                95,
                1,
                14,
                List.of(),
                List.of("Anomaly Radar", "UPI Collect Guard"));

        when(fraudService.getSecurityCenter(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/fraud-center/security-center")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.defenseStatus").value("DEFCON 2 - ELEVATED VIGILANCE"))
                .andExpect(jsonPath("$.data.systemShieldLevel").value(95))
                .andExpect(jsonPath("$.data.activeAlertsCount").value(1));
    }

    @Test
    void scanPhishing_detectsScamHeuristics() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        PhishingScanResponse response = new PhishingScanResponse(
                "http://bit.ly/bank-kyc",
                88,
                true,
                "URGENT_KYC_PHISHING",
                "Urgent account suspension threat with unverified link",
                "Do not click or approve UPI requests");

        when(fraudService.scanPhishing(any())).thenReturn(response);

        mockMvc.perform(post("/api/fraud-center/scan-phishing")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "inputQuery": "URGENT: Click http://bit.ly/bank-kyc or your account will be suspended in 24 hours!"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isScam").value(true))
                .andExpect(jsonPath("$.data.riskScore").value(88))
                .andExpect(jsonPath("$.data.threatType").value("URGENT_KYC_PHISHING"));
    }

    @Test
    void takeFraudAction_handlesDefensiveAction() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        FraudActionResponse actionResponse = new FraudActionResponse(
                "HDFC-1294",
                "FREEZE_CARD",
                "Card HDFC Regalia Gold (**1294) has been instantly frozen across all online & POS terminals.",
                "SUCCESS");

        when(fraudService.handleFraudAction(anyLong(), any())).thenReturn(actionResponse);

        mockMvc.perform(post("/api/fraud-center/action")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "transactionId": "HDFC-1294",
                            "action": "FREEZE_CARD"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionTaken").value("FREEZE_CARD"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void getReport_generatesInvestigationReport() throws Exception {
        UserPrincipal principal = createPrincipal(1L);
        FraudInvestigationReportResponse report = new FraudInvestigationReportResponse(
                "REP-202610-8472",
                "2026-10-10T03:00:00Z",
                "Suspicious High-Velocity Transactions",
                "Ada Lovelace",
                List.of(),
                List.of("Amount: 3.4x usual", "Time: 02:40 AM"),
                List.of("Freeze card", "Dispute transaction"),
                "DISP-98124");

        when(fraudService.generateInvestigationReport(anyLong())).thenReturn(report);

        mockMvc.perform(get("/api/fraud-center/investigation-report")
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportId").value("REP-202610-8472"))
                .andExpect(jsonPath("$.data.caseTitle").value("Suspicious High-Velocity Transactions"));
    }
}
