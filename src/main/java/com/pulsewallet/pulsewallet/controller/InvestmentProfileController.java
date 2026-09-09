package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentProfileRequest;
import com.pulsewallet.pulsewallet.dto.InvestmentProfileResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.InvestmentProfileService;

@RestController
@RequestMapping("/api/investment-profile")
public class InvestmentProfileController {

    private final InvestmentProfileService profileService;

    public InvestmentProfileController(InvestmentProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<InvestmentProfileResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(profileService.get(requirePrincipal(principal).getId()));
    }

    @PutMapping
    public ApiResponse<InvestmentProfileResponse> save(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody InvestmentProfileRequest request) {
        return ApiResponse.ok(profileService.save(requirePrincipal(principal).getId(), request));
    }

    private UserPrincipal requirePrincipal(UserPrincipal principal) {
        if (principal == null)
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        return principal;
    }
}
