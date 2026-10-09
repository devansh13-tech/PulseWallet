package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.FraudHubDtos.*;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.AdvancedFraudService;

@RestController
@RequestMapping("/api/fraud-center")
public class AdvancedFraudController {

    private final AdvancedFraudService fraudService;

    public AdvancedFraudController(AdvancedFraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/security-center")
    public ApiResponse<SecurityCenterResponse> getSecurityCenter(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(fraudService.getSecurityCenter(principal != null ? principal.getId() : 1L));
    }

    @PostMapping("/scan-phishing")
    public ApiResponse<PhishingScanResponse> scanPhishing(@RequestBody PhishingScanRequest request) {
        return ApiResponse.ok(fraudService.scanPhishing(request));
    }

    @PostMapping("/action")
    public ApiResponse<FraudActionResponse> takeFraudAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody FraudActionRequest request) {
        return ApiResponse.ok(fraudService.handleFraudAction(principal != null ? principal.getId() : 1L, request));
    }

    @GetMapping("/investigation-report")
    public ApiResponse<FraudInvestigationReportResponse> getReport(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(fraudService.generateInvestigationReport(principal != null ? principal.getId() : 1L));
    }
}
