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
import com.pulsewallet.pulsewallet.dto.FinancialGoalRequest;
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.GoalAchievementResponse;
import com.pulsewallet.pulsewallet.dto.GoalPlanResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.FinancialGoalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/goals")
public class FinancialGoalController {

    private final FinancialGoalService financialGoalService;

    public FinancialGoalController(FinancialGoalService financialGoalService) {
        this.financialGoalService = financialGoalService;
    }

    @GetMapping
    public ApiResponse<List<FinancialGoalResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financialGoalService.list(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<FinancialGoalResponse> get(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financialGoalService.get(id, principal.getId()));
    }

    @GetMapping("/{id}/plan")
    public ApiResponse<GoalPlanResponse> getPlan(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financialGoalService.getPlan(id, principal.getId()));
    }

    @GetMapping("/{id}/achievement")
    public ApiResponse<GoalAchievementResponse> getAchievement(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(financialGoalService.getAchievement(id, principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FinancialGoalResponse> create(@Valid @RequestBody FinancialGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Financial goal created", financialGoalService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<FinancialGoalResponse> update(@PathVariable Long id,
            @Valid @RequestBody FinancialGoalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Financial goal updated", financialGoalService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        financialGoalService.delete(id, principal.getId());
        return ApiResponse.message("Financial goal deleted");
    }
}