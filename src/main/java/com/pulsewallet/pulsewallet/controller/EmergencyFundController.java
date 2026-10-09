package com.pulsewallet.pulsewallet.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.EmergencyFundService;

@RestController
@RequestMapping("/api/emergency-fund")
public class EmergencyFundController {

    private final EmergencyFundService emergencyFundService;

    public EmergencyFundController(EmergencyFundService emergencyFundService) {
        this.emergencyFundService = emergencyFundService;
    }

    @GetMapping
    public ApiResponse<EmergencyFundPlanResponse> getPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal currentAmount,
            @RequestParam(required = false) Integer months) {
        return ApiResponse.ok(emergencyFundService.calculate(principal.getId(), from, to, currentAmount, months));
    }
}
