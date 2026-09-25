package com.pulsewallet.pulsewallet.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.AdvisoryResponse;
import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.AdvisoryService;

@RestController
@RequestMapping("/api")
public class AdvisoryController {

    private final AdvisoryService advisoryService;

    public AdvisoryController(AdvisoryService advisoryService) {
        this.advisoryService = advisoryService;
    }

    @GetMapping("/advisory")
    public ApiResponse<AdvisoryResponse> advisory(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(advisoryService.advice(principal.getId()));
    }
}
