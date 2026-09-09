package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.RecommendationResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.RecommendationService;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public ApiResponse<RecommendationResponse> recommendations(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return ApiResponse.ok(recommendationService.recommend(principal.getId()));
    }
}