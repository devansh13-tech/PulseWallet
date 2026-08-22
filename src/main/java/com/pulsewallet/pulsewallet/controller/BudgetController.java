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
import com.pulsewallet.pulsewallet.dto.BudgetRequest;
import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.BudgetService;

import jakarta.validation.Valid;

/**
 * Milestone 2 CRUD only - a limit, an optional category, and a date range.
 * The Milestone 3 budget-recommendation engine is a separate concern and
 * lives elsewhere once it exists.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ApiResponse<List<BudgetResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(budgetService.list(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> get(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(budgetService.get(id, principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BudgetResponse> create(
            @Valid @RequestBody BudgetRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Budget created", budgetService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BudgetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Budget updated", budgetService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        budgetService.delete(id, principal.getId());
        return ApiResponse.message("Budget deleted");
    }
}
