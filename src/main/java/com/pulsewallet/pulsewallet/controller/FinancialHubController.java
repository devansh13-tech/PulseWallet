package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.FinancialHubDtos.*;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.FinancialHubService;

@RestController
@RequestMapping("/api/financial-hub")
public class FinancialHubController {

    private final FinancialHubService hubService;

    public FinancialHubController(FinancialHubService hubService) {
        this.hubService = hubService;
    }

    @GetMapping("/proactive-insights")
    public ApiResponse<ProactiveInsightsResponse> getInsights(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(hubService.getProactiveInsights(principal != null ? principal.getId() : 1L));
    }

    @GetMapping("/cibil-score")
    public ApiResponse<CibilCreditScoreResponse> getCibilScore(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(hubService.getCibilCreditScore(principal != null ? principal.getId() : 1L));
    }

    @GetMapping("/debt-payoff")
    public ApiResponse<DebtPayoffComparisonResponse> getDebtPayoff(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(hubService.getDebtPayoffComparison(principal != null ? principal.getId() : 1L));
    }

    @GetMapping("/net-worth")
    public ApiResponse<NetWorthBreakdownResponse> getNetWorth(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(hubService.getNetWorthBreakdown(principal != null ? principal.getId() : 1L));
    }

    @GetMapping("/tax-tracker")
    public ApiResponse<TaxTrackerResponse> getTaxTracker(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(hubService.getTaxTracker(principal != null ? principal.getId() : 1L));
    }
}
