package com.pulsewallet.pulsewallet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.dto.DashboardSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FraudAlertSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FraudSummaryResponse;
import com.pulsewallet.pulsewallet.repository.FraudAlertRepository;

@Service
public class DashboardSummaryService {

    private final FinancialSummaryService financialSummaryService;
    private final BudgetService budgetService;
    private final FraudAlertRepository fraudAlertRepository;

    public DashboardSummaryService(
            FinancialSummaryService financialSummaryService,
            BudgetService budgetService,
            FraudAlertRepository fraudAlertRepository) {
        this.financialSummaryService = financialSummaryService;
        this.budgetService = budgetService;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        FinancialSummaryResponse financialSummary = financialSummaryService.summarize(userId, null, null);
        List<BudgetResponse> budgets = budgetService.list(userId);
        long totalAlerts = fraudAlertRepository.countByUserId(userId);
        long unresolvedAlerts = fraudAlertRepository.countByUserIdAndResolvedFalse(userId);
        List<FraudAlertSummaryResponse> recentAlerts = fraudAlertRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FraudAlertSummaryResponse::from)
                .toList();

        return new DashboardSummaryResponse(
                financialSummary,
                budgets,
                new FraudSummaryResponse(totalAlerts, unresolvedAlerts, recentAlerts));
    }
}
