package com.pulsewallet.pulsewallet.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.ExchangeRateService;

@RestController
@RequestMapping("/api")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/exchange-rates")
    public ApiResponse<Map<String, Object>> rates(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "INR") String base) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required");
        }

        Map<String, Object> payload = Map.of(
                "base", exchangeRateService.getBaseCurrency(),
                "rates", exchangeRateService.getRates(base),
                "updatedAt", System.currentTimeMillis());
        return ApiResponse.ok(payload);
    }
}
