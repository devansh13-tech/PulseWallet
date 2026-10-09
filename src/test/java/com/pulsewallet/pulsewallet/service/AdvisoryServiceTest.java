package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.AdvisoryResponse;
import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.GoalAchievementResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.dto.MonthlyAdviceResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.FinancialGoalType;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.MonthlyTypeTotalProjection;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class AdvisoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FinancialSummaryService financialSummaryService;
    @Mock
    private EmergencyFundService emergencyFundService;
    @Mock
    private FinancialGoalService financialGoalService;
    @Mock
    private FinancialGoalRepository financialGoalRepository;
    @Mock
    private InvestmentRecommendationService investmentRecommendationService;

    private AdvisoryService advisoryService;

    @BeforeEach
    void setUp() {
        advisoryService = new AdvisoryService(
                transactionRepository,
                Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneId.of("UTC")),
                financialSummaryService,
                emergencyFundService,
                financialGoalService,
                financialGoalRepository,
                investmentRecommendationService);
    }

    @Test
    void advisory_handlesHealthyDisposableIncome() {
        when(transactionRepository.sumByMonthAndType(eq(1L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 7, TransactionType.INCOME, new BigDecimal("120000.00")),
                        monthTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("60000.00")),
                        monthTotal(2026, 8, TransactionType.INCOME, new BigDecimal("120000.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("65000.00"))));

        AdvisoryResponse response = advisoryService.advice(1L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("115000.00");
        assertThat(response.emergencyFundTarget()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.recommendedSavings()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.recommendedInvestment()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.guidance()).contains("illustrative");
    }

    @Test
    void advisory_handlesZeroDisposableIncome() {
        when(transactionRepository.sumByMonthAndType(eq(2L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 8, TransactionType.INCOME, new BigDecimal("80000.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("80000.00"))));

        AdvisoryResponse response = advisoryService.advice(2L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedSavings()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedInvestment()).isEqualByComparingTo("0.00");
    }

    @Test
    void advisory_handlesNegativeDisposableIncome() {
        when(transactionRepository.sumByMonthAndType(eq(3L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 8, TransactionType.INCOME, new BigDecimal("50000.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("70000.00"))));

        AdvisoryResponse response = advisoryService.advice(3L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("-20000.00");
        assertThat(response.recommendedSavings()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedInvestment()).isEqualByComparingTo("0.00");
    }

    @Test
    void advisory_handlesHighExpensesAndLowIncome() {
        when(transactionRepository.sumByMonthAndType(eq(4L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 8, TransactionType.INCOME, new BigDecimal("10000.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("9500.00"))));

        AdvisoryResponse response = advisoryService.advice(4L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("500.00");
        assertThat(response.emergencyFundTarget()).isEqualByComparingTo("57000.00");
        assertThat(response.shortTermReserveTarget()).isEqualByComparingTo("28500.00");
        assertThat(response.recommendedSavings()).isEqualByComparingTo("150.00");
        assertThat(response.recommendedInvestment()).isEqualByComparingTo("75.00");
    }

    @Test
    void advisory_handlesPositiveSavingsCapacity() {
        when(transactionRepository.sumByMonthAndType(eq(6L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 8, TransactionType.INCOME, new BigDecimal("100000.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("50000.00"))));

        AdvisoryResponse response = advisoryService.advice(6L);

        assertThat(response.recommendedSavings()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.recommendedInvestment()).isGreaterThan(BigDecimal.ZERO);
    }

    // ==========================================
    // M5.10 Personalized Monthly Financial Advice Tests
    // ==========================================

    @Test
    void healthyFinancialSituationAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("100000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("40.00"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("12000.00"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                "Bike",
                "ON_TRACK",
                BigDecimal.ZERO);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_6");
        assertThat(response.advice()).contains("cash flow is positive").contains("emergency fund is ready");
        assertThat(response.emergencyFundReady()).isTrue();
        assertThat(response.emergencyFundRemaining()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("12000.00");
        assertThat(response.diversified()).isTrue();
        assertThat(response.diversificationStatus()).isEqualTo("DIVERSIFIED");
        assertThat(response.recommendations()).anyMatch(r -> r.contains("Continue your recommended monthly investment"));
    }

    @Test
    void emergencyFundIncompleteAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("80000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("37.50"),
                false,
                new BigDecimal("60000.00"),
                new BigDecimal("4500.00"),
                InvestmentRiskPreference.CONSERVATIVE,
                true,
                "DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_1");
        assertThat(response.advice()).contains("emergency fund is not fully funded").contains("Prioritize");
        assertThat(response.emergencyFundReady()).isFalse();
        assertThat(response.emergencyFundRemaining()).isEqualByComparingTo("60000.00");
        assertThat(response.warnings()).anyMatch(w -> w.contains("Emergency fund is not fully funded"));
        assertThat(response.recommendations()).anyMatch(r -> r.contains("building the remaining emergency reserve"));
    }

    @Test
    void zeroDisposableIncomeAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("50000.00"),
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_2");
        assertThat(response.advice()).contains("expenses currently consume all of your income");
        assertThat(response.disposableIncome()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("0.00");
        assertThat(response.warnings()).anyMatch(w -> w.contains("Disposable income is zero or negative"));
    }

    @Test
    void negativeDisposableIncomeAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("50000.00"),
                new BigDecimal("65000.00"),
                new BigDecimal("-15000.00"),
                new BigDecimal("-30.00"),
                true,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_2");
        assertThat(response.advice()).contains("expenses currently exceed your income");
        assertThat(response.disposableIncome()).isEqualByComparingTo("-15000.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("0.00");
        assertThat(response.recommendations()).anyMatch(r -> r.contains("eliminate the monthly deficit"));
    }

    @Test
    void goalAtRiskAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("100000.00"),
                new BigDecimal("70000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("30.00"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                "House Downpayment",
                "AT_RISK",
                new BigDecimal("6000.00"));

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_3");
        assertThat(response.advice()).contains("investment capacity is below the monthly contribution required");
        assertThat(response.primaryGoalName()).isEqualTo("House Downpayment");
        assertThat(response.primaryGoalStatus()).isEqualTo("AT_RISK");
        assertThat(response.primaryGoalShortfall()).isEqualByComparingTo("6000.00");
        assertThat(response.warnings()).anyMatch(w -> w.contains("Monthly shortfall of ₹6000.00"));
        assertThat(response.recommendations()).anyMatch(r -> r.contains("Increase monthly allocation by ₹6000.00"));
    }

    @Test
    void goalOnTrackAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("100000.00"),
                new BigDecimal("70000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("30.00"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                "Vacation",
                "ON_TRACK",
                BigDecimal.ZERO);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_6");
        assertThat(response.primaryGoalName()).isEqualTo("Vacation");
        assertThat(response.primaryGoalStatus()).isEqualTo("ON_TRACK");
        assertThat(response.primaryGoalShortfall()).isEqualByComparingTo("0.00");
        assertThat(response.recommendations()).anyMatch(r -> r.contains("Goal 'Vacation' is on track"));
    }

    @Test
    void noActiveGoalAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("80000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("37.50"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.primaryGoalName()).isNull();
        assertThat(response.primaryGoalStatus()).isNull();
        assertThat(response.primaryGoalShortfall()).isNull();
        assertThat(response.advicePriority()).isEqualTo("PRIORITY_6");
    }

    @Test
    void investmentRecommendationIncludedAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("80000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("37.50"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                InvestmentRiskPreference.AGGRESSIVE,
                true,
                "DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("9000.00");
        assertThat(response.riskPreference()).isEqualTo(InvestmentRiskPreference.AGGRESSIVE);
    }

    @Test
    void diversificationInformationIncludedAdvice() {
        // Not diversified triggers Priority 5
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("80000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("37.50"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                InvestmentRiskPreference.MODERATE,
                false,
                "PARTIALLY_DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_5");
        assertThat(response.diversified()).isFalse();
        assertThat(response.diversificationStatus()).isEqualTo("PARTIALLY_DIVERSIFIED");
        assertThat(response.advice()).contains("asset allocation is not fully diversified");
        assertThat(response.recommendations()).anyMatch(r -> r.contains("multi-asset classes"));
    }

    @Test
    void correctSavingsRateAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("120000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("25.00"),
                true,
                BigDecimal.ZERO,
                new BigDecimal("9000.00"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.savingsRate()).isEqualByComparingTo("25.00");
        assertThat(response.totalIncome()).isEqualByComparingTo("120000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("90000.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("30000.00");
    }

    @Test
    void userIsolationInMonthlyAdvice() {
        assertThatThrownBy(() -> advisoryService.monthlyAdvice(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId must not be null");
    }

    @Test
    void bigDecimalPrecisionInMonthlyAdvice() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("33333.333"),
                new BigDecimal("11111.111"),
                new BigDecimal("22222.222"),
                new BigDecimal("66.666"),
                true,
                new BigDecimal("0.001"),
                new BigDecimal("6666.666"),
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                "Goal",
                "ON_TRACK",
                new BigDecimal("0.001"));

        assertThat(response.totalIncome()).isEqualByComparingTo("33333.33");
        assertThat(response.totalExpenses()).isEqualByComparingTo("11111.11");
        assertThat(response.disposableIncome()).isEqualByComparingTo("22222.22");
        assertThat(response.savingsRate()).isEqualByComparingTo("66.67");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6666.67");
    }

    @Test
    void noNegativeRecommendationValues() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                new BigDecimal("50000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("-10000.00"),
                new BigDecimal("-20.00"),
                false,
                new BigDecimal("-500.00"), // negative input clamped to 0
                new BigDecimal("-1000.00"), // negative input clamped to 0
                InvestmentRiskPreference.MODERATE,
                true,
                "DIVERSIFIED",
                null,
                null,
                new BigDecimal("-200.00"));

        assertThat(response.emergencyFundRemaining()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(response.recommendedMonthlyInvestment()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void noTransactionsEmptyFinancialData() {
        MonthlyAdviceResponse response = advisoryService.calculateMonthlyAdvice(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                InvestmentRiskPreference.MODERATE,
                false,
                "NOT_DIVERSIFIED",
                null,
                null,
                null);

        assertThat(response.advicePriority()).isEqualTo("PRIORITY_2");
        assertThat(response.advice()).contains("No financial activity recorded yet");
        assertThat(response.warnings()).anyMatch(w -> w.contains("No transactions found"));
        assertThat(response.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
    }

    @Test
    void monthlyAdviceOrchestratesAllServices() {
        Long userId = 10L;
        FinancialSummaryResponse mockSummary = new FinancialSummaryResponse(
                null, null,
                new BigDecimal("100000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("40.00"),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        when(financialSummaryService.summarize(userId, null, null)).thenReturn(mockSummary);

        EmergencyFundPlanResponse mockEf = new EmergencyFundPlanResponse(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 8, 15),
                new BigDecimal("30000.00"),
                6,
                new BigDecimal("180000.00"),
                new BigDecimal("180000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("40000.00"),
                new BigDecimal("100.00"),
                List.of());
        when(emergencyFundService.calculate(userId)).thenReturn(mockEf);

        InvestmentRecommendationResponse mockRec = new InvestmentRecommendationResponse(
                new BigDecimal("12000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("30.00"),
                true,
                BigDecimal.ZERO,
                InvestmentRiskPreference.MODERATE,
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                new BigDecimal("10.00"),
                new BigDecimal("7200.00"),
                new BigDecimal("3600.00"),
                new BigDecimal("1200.00"),
                true,
                "DIVERSIFIED",
                "Diversified portfolio",
                null, null, null, null, null, null,
                "Reason",
                List.of());
        when(investmentRecommendationService.recommend(userId)).thenReturn(mockRec);

        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), userId);
        FinancialGoal goal = TestEntities.withId(
                new FinancialGoal(user, "House", FinancialGoalType.HOUSE,
                        new BigDecimal("2000000.00"), new BigDecimal("200000.00"),
                        LocalDate.of(2034, 9, 4), "Dream home"),
                1L);
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(userId)).thenReturn(List.of(goal));

        GoalAchievementResponse mockAchievement = new GoalAchievementResponse(
                1L,
                "House",
                new BigDecimal("2000000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("1800000.00"),
                LocalDate.of(2034, 9, 4),
                new BigDecimal("10.00"),
                96,
                new BigDecimal("18750.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("12000.00"),
                new BigDecimal("6750.00"),
                new BigDecimal("0.00"),
                new BigDecimal("1500000.00"),
                "AT_RISK",
                false,
                "Goal is at risk",
                List.of("Shortfall"));
        when(financialGoalService.getAchievement(1L, userId)).thenReturn(mockAchievement);

        MonthlyAdviceResponse response = advisoryService.monthlyAdvice(userId);

        assertThat(response.totalIncome()).isEqualByComparingTo("100000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("60000.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("40000.00");
        assertThat(response.savingsRate()).isEqualByComparingTo("40.00");
        assertThat(response.emergencyFundReady()).isTrue();
        assertThat(response.emergencyFundRemaining()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("12000.00");
        assertThat(response.riskPreference()).isEqualTo(InvestmentRiskPreference.MODERATE);
        assertThat(response.diversified()).isTrue();
        assertThat(response.primaryGoalName()).isEqualTo("House");
        assertThat(response.primaryGoalStatus()).isEqualTo("AT_RISK");
        assertThat(response.primaryGoalShortfall()).isEqualByComparingTo("6750.00");
        assertThat(response.advicePriority()).isEqualTo("PRIORITY_3");
        assertThat(response.advice()).contains("investment capacity is below the monthly contribution required");

        verify(financialSummaryService).summarize(userId, null, null);
        verify(emergencyFundService).calculate(userId);
        verify(investmentRecommendationService).recommend(userId);
        verify(financialGoalRepository).findByUserIdOrderByTargetDateAsc(userId);
        verify(financialGoalService).getAchievement(1L, userId);
    }

    private MonthlyTypeTotalProjection monthTotal(int year, int month, TransactionType type, BigDecimal total) {
        return new MonthlyTypeTotalProjection() {
            @Override
            public Integer getYear() {
                return year;
            }

            @Override
            public Integer getMonth() {
                return month;
            }

            @Override
            public TransactionType getType() {
                return type;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }
        };
    }
}
