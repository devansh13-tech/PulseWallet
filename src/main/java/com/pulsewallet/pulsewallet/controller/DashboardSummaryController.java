package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.DashboardSummaryResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.DashboardSummaryService;

@RestController
@RequestMapping("/api")
public class DashboardSummaryController {

    private final DashboardSummaryService dashboardSummaryService;

    public DashboardSummaryController(DashboardSummaryService dashboardSummaryService) {
        this.dashboardSummaryService = dashboardSummaryService;
    }

    @GetMapping("/dashboard-summary")
    public ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(dashboardSummaryService.getSummary(requirePrincipal(principal).getId()));
    }

    private UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal;
    }
}
