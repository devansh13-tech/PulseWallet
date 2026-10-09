package com.pulsewallet.pulsewallet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.LinkedPaymentAccountRequest;
import com.pulsewallet.pulsewallet.dto.LinkedPaymentAccountResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.LinkedPaymentAccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/linked-accounts")
public class LinkedPaymentAccountController {

    private final LinkedPaymentAccountService accountService;

    public LinkedPaymentAccountController(LinkedPaymentAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<List<LinkedPaymentAccountResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(accountService.list(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<LinkedPaymentAccountResponse> get(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(accountService.get(id, principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LinkedPaymentAccountResponse> create(@Valid @RequestBody LinkedPaymentAccountRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Payment account linked", accountService.create(principal.getId(), request));
    }

    @PatchMapping("/{id}/disconnect")
    public ApiResponse<LinkedPaymentAccountResponse> disconnect(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Payment account disconnected", accountService.disconnect(id, principal.getId()));
    }

    public record UpdateBalanceRequest(java.math.BigDecimal balance) {}

    @PatchMapping("/{id}/balance")
    public ApiResponse<LinkedPaymentAccountResponse> updateBalance(
            @PathVariable Long id,
            @RequestBody UpdateBalanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Account balance updated",
                accountService.updateBalance(id, principal.getId(), request != null ? request.balance() : java.math.BigDecimal.ZERO));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        accountService.delete(id, principal.getId());
        return ApiResponse.message("Linked payment account deleted");
    }
}
