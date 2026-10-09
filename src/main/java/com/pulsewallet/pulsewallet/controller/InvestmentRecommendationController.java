package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentProjectionResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.InvestmentRecommendationService;

@RestController
@RequestMapping("/api/investments")
public class InvestmentRecommendationController {

    private final InvestmentRecommendationService investmentRecommendationService;

    public InvestmentRecommendationController(InvestmentRecommendationService investmentRecommendationService) {
        this.investmentRecommendationService = investmentRecommendationService;
    }

    @GetMapping("/recommendation")
    public ApiResponse<InvestmentRecommendationResponse> getRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long goalId) {
        return ApiResponse.ok(investmentRecommendationService.recommend(principal.getId(), goalId));
    }

    @GetMapping("/projection")
    public ApiResponse<InvestmentProjectionResponse> getProjection(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long goalId,
            @RequestParam(required = false, defaultValue = "10") Integer years) {
        return ApiResponse.ok(investmentRecommendationService.project(principal.getId(), goalId, years));
    }
}
