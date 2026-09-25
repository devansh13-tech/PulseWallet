package com.pulsewallet.pulsewallet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardRequest;
import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckRequest;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.CreditCardIntelligenceService;
import com.pulsewallet.pulsewallet.service.CreditCardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardIntelligenceService intelligenceService;

    public CreditCardController(CreditCardService creditCardService,
            CreditCardIntelligenceService intelligenceService) {
        this.creditCardService = creditCardService;
        this.intelligenceService = intelligenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreditCardResponse> create(@Valid @RequestBody CreditCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Credit card created", creditCardService.create(principal.getId(), request));
    }

    @GetMapping
    public ApiResponse<List<CreditCardResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(creditCardService.getAll(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CreditCardResponse> get(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(creditCardService.getById(id, principal.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<CreditCardResponse> update(@PathVariable Long id,
            @Valid @RequestBody CreditCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Credit card updated", creditCardService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        creditCardService.delete(id, principal.getId());
        return ApiResponse.message("Credit card deleted");
    }

    @GetMapping("/intelligence")
    public ApiResponse<CreditCardIntelligenceResponse> intelligence(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(intelligenceService.getIntelligence(principal.getId()));
    }

    @PostMapping("/purchase-check")
    public ApiResponse<PurchaseCheckResponse> purchaseCheck(@Valid @RequestBody PurchaseCheckRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(intelligenceService.checkPurchase(principal.getId(), request));
    }
}
