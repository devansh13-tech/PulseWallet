package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.FinancialPlanResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.FinancialPlanService;

@RestController
@RequestMapping("/api/financial-plan")
public class FinancialPlanController {

    private final FinancialPlanService financialPlanService;

    public FinancialPlanController(FinancialPlanService financialPlanService) {
        this.financialPlanService = financialPlanService;
    }

    @GetMapping
    public ApiResponse<FinancialPlanResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null)
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        return ApiResponse.ok(financialPlanService.calculate(principal.getId()));
    }
}
