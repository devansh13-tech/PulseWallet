package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.dto.DashboardSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.FraudAlert;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.repository.FraudAlertRepository;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {

    @Mock
    private FinancialSummaryService financialSummaryService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private DashboardSummaryService dashboardSummaryService;

    @Test
    void getSummary_returnsUserScopedFinancialAndFraudData() {
        Long userId = 42L;
        FinancialSummaryResponse financialSummary = new FinancialSummaryResponse(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("2500.00"),
                new BigDecimal("1600.00"),
                new BigDecimal("900.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        BudgetResponse budgetResponse = new BudgetResponse(
                9L,
                new BigDecimal("1200.00"),
                5L,
                "Housing",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
        FraudAlert alert = mock(FraudAlert.class);
        Transaction transaction = mock(Transaction.class);
        when(alert.getId()).thenReturn(8L);
        when(alert.getTransaction()).thenReturn(transaction);
        when(transaction.getId()).thenReturn(100L);
        when(alert.getRiskLevel()).thenReturn(FraudAlert.RiskLevel.HIGH);
        when(alert.getFraudProbability()).thenReturn(new BigDecimal("0.87"));
        when(alert.getRiskScore()).thenReturn(new BigDecimal("87.00"));
        when(alert.isResolved()).thenReturn(false);
        when(alert.getCreatedAt()).thenReturn(Instant.parse("2026-08-30T10:00:00Z"));

        when(financialSummaryService.summarize(userId, null, null)).thenReturn(financialSummary);
        when(budgetService.list(userId)).thenReturn(List.of(budgetResponse));
        when(fraudAlertRepository.countByUserId(userId)).thenReturn(3L);
        when(fraudAlertRepository.countByUserIdAndResolvedFalse(userId)).thenReturn(2L);
        when(fraudAlertRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(alert));

        DashboardSummaryResponse result = dashboardSummaryService.getSummary(userId);

        assertThat(result.financialSummary()).isEqualTo(financialSummary);
        assertThat(result.budgets()).containsExactly(budgetResponse);
        assertThat(result.fraudSummary().totalAlerts()).isEqualTo(3L);
        assertThat(result.fraudSummary().unresolvedAlerts()).isEqualTo(2L);
        assertThat(result.fraudSummary().recentAlerts()).hasSize(1);
        assertThat(result.fraudSummary().recentAlerts().get(0).transactionId()).isEqualTo(100L);

        verify(financialSummaryService).summarize(userId, null, null);
        verify(budgetService).list(userId);
        verify(fraudAlertRepository).countByUserId(userId);
        verify(fraudAlertRepository).countByUserIdAndResolvedFalse(userId);
        verify(fraudAlertRepository).findTop5ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getSummary_handlesEmptyDashboardGracefully() {
        Long userId = 88L;
        when(financialSummaryService.summarize(userId, null, null))
                .thenReturn(new FinancialSummaryResponse(
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));
        when(budgetService.list(userId)).thenReturn(List.of());
        when(fraudAlertRepository.countByUserId(userId)).thenReturn(0L);
        when(fraudAlertRepository.countByUserIdAndResolvedFalse(userId)).thenReturn(0L);
        when(fraudAlertRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        DashboardSummaryResponse result = dashboardSummaryService.getSummary(userId);

        assertThat(result.financialSummary().totalIncome()).isZero();
        assertThat(result.budgets()).isEmpty();
        assertThat(result.fraudSummary().totalAlerts()).isZero();
        assertThat(result.fraudSummary().unresolvedAlerts()).isZero();
        assertThat(result.fraudSummary().recentAlerts()).isEmpty();
    }
}
