package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.FraudAlertRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private FinancialSummaryService financialSummaryService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private FinancialGoalService financialGoalService;

    @Test
    void recommendsBufferFromObservedSurplusAndKeepsInvestmentsUnassigned() {
        Long userId = 7L;
        when(financialSummaryService.summarize(userId, null, null)).thenReturn(summary(
                new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME, money("80000")),
                new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE, money("35000"))));
        when(budgetService.list(userId)).thenReturn(List.of());
        when(fraudAlertRepository.countByUserIdAndResolvedFalse(userId)).thenReturn(0L);
        when(financialGoalService.list(userId)).thenReturn(List.of());

        var result = service().recommend(userId);

        assertThat(result.monthlyIncome()).isEqualByComparingTo("80000.00");
        assertThat(result.totalExpenses()).isEqualByComparingTo("35000.00");
        assertThat(result.monthlyLeft()).isEqualByComparingTo("45000.00");
        assertThat(result.buffer()).isEqualByComparingTo("35000.00");
        assertThat(result.monthlySavings()).isEqualByComparingTo("35000.00");
        assertThat(result.investments()).isZero();
        assertThat(result.flexibleSpending()).isEqualByComparingTo("10000.00");
        assertThat(result.engine()).isEqualTo("deterministic-rules-v1");
    }

    @Test
    void emitsWarningsForNegativeLeftAndUnresolvedAlerts() {
        Long userId = 8L;
        when(financialSummaryService.summarize(userId, null, null)).thenReturn(summary(
                new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME, money("1000")),
                new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE, money("1400"))));
        when(budgetService.list(userId)).thenReturn(List.of());
        when(fraudAlertRepository.countByUserIdAndResolvedFalse(userId)).thenReturn(2L);
        when(financialGoalService.list(userId)).thenReturn(List.of());

        var result = service().recommend(userId);

        assertThat(result.monthlyLeft()).isEqualByComparingTo("-400.00");
        assertThat(result.recommendations()).extracting("type")
                .contains("WARNING");
        assertThat(result.recommendations()).anyMatch(item -> item.message().contains("2 unresolved"));
    }

    @Test
    void flagsLatestCategorySpendingAboveUserBudget() {
        Long userId = 9L;
        when(financialSummaryService.summarize(userId, null, null)).thenReturn(new FinancialSummaryResponse(
                null, null, money("1000"), money("600"), money("400"), List.of(), List.of(), List.of(), List.of(),
                List.of(new FinancialSummaryResponse.CategoryMonthlyAmount(2026, 8, 3L, "Food", money("700"))),
                List.of(new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME, money("1000")),
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE,
                                money("600")))));
        when(budgetService.list(userId)).thenReturn(List.of(new BudgetResponse(1L, money("500"), 3L, "Food",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), Instant.EPOCH, Instant.EPOCH)));
        when(fraudAlertRepository.countByUserIdAndResolvedFalse(userId)).thenReturn(0L);
        when(financialGoalService.list(userId)).thenReturn(List.of());

        var result = service().recommend(userId);

        assertThat(result.recommendations()).anyMatch(item -> "SPENDING_RECOMMENDATION".equals(item.type()));
    }

    @Test
    void usesActiveGoalRequirementInSavingsAllocationAndMessage() {
        Long userId = 10L;
        when(financialSummaryService.summarize(userId, null, null)).thenReturn(summary(
                new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME, money("80000")),
                new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE, money("35000"))));
        when(budgetService.list(userId)).thenReturn(List.of());
        when(fraudAlertRepository.countByUserIdAndResolvedFalse(userId)).thenReturn(0L);
        when(financialGoalService.list(userId)).thenReturn(List.of(new FinancialGoalResponse(2L, "Bike", "BIKE",
                money("200000"), money("40000"), money("160000"), money("20"), 10, money("16000"),
                "ON_TRACK", LocalDate.of(2027, 7, 4), null, null)));

        var result = service().recommend(userId);

        assertThat(result.goalSavings()).isEqualByComparingTo("16000");
        assertThat(result.monthlySavings()).isEqualByComparingTo("45000");
        assertThat(result.flexibleSpending()).isEqualByComparingTo("0");
        assertThat(result.recommendations()).anyMatch(item -> item.message().contains("active goals require"));
    }

    private RecommendationService service() {
        return new RecommendationService(financialSummaryService, budgetService, fraudAlertRepository,
                financialGoalService,
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneId.of("UTC")));
    }

    private FinancialSummaryResponse summary(FinancialSummaryResponse.MonthlyTypeAmount... rows) {
        return new FinancialSummaryResponse(null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(rows));
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}