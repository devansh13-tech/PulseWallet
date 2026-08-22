package com.pulsewallet.pulsewallet.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.TransactionRequest;
import com.pulsewallet.pulsewallet.dto.TransactionResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.TransactionService;

import jakarta.validation.Valid;

/**
 * All endpoints require a valid JWT (see {@code SecurityConfig}). The
 * authenticated user's id comes only from {@code @AuthenticationPrincipal} -
 * never from a path or body parameter - so a caller cannot ask to act as
 * another user, and a transaction id belonging to someone else 404s rather
 * than 403s (see {@code ResourceNotFoundException}'s Javadoc for why).
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Most recent first by default; standard Spring
     * {@code page}/{@code size}/{@code sort} params override it.
     */
    @GetMapping
    public ApiResponse<Page<TransactionResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(transactionService.list(principal.getId(), from, to, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(transactionService.get(id, principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Transaction created", transactionService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Transaction updated", transactionService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        transactionService.delete(id, principal.getId());
        return ApiResponse.message("Transaction deleted");
    }
}
