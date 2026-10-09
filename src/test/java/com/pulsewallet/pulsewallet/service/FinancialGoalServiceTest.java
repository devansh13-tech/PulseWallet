package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.FinancialGoalRequest;
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.GoalAchievementResponse;
import com.pulsewallet.pulsewallet.dto.GoalPlanResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.FinancialGoalType;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class FinancialGoalServiceTest {

    @Mock
    private FinancialGoalRepository goalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FinancialSummaryService summaryService;
    @Mock
    private InvestmentRecommendationService investmentRecommendationService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC);
    private FinancialGoalService service;

    @BeforeEach
    void setUp() {
        service = new FinancialGoalService(goalRepository, userRepository, summaryService, clock, investmentRecommendationService);
    }

    @Test
    void createsGoalWithValidatedInputAndCalculatedFields() {
        when(summaryService.summarize(1L, null, null)).thenReturn(summary(80000, 35000));
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        FinancialGoal saved = new FinancialGoal(user, "Bike", FinancialGoalType.BIKE,
                new BigDecimal("200000"), new BigDecimal("40000"), LocalDate.of(2027, 7, 4), null);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(goalRepository.save(org.mockito.ArgumentMatchers.any(FinancialGoal.class))).thenReturn(saved);

        FinancialGoalResponse result = service.create(1L, new FinancialGoalRequest("Bike", FinancialGoalType.BIKE,
                new BigDecimal("200000"), new BigDecimal("40000"), LocalDate.of(2027, 7, 4), null));

        assertThat(result.remainingAmount()).isEqualByComparingTo("160000");
        assertThat(result.progressPercentage()).isEqualByComparingTo("20.00");
        assertThat(result.remainingMonths()).isEqualTo(10);
        assertThat(result.requiredMonthlySaving()).isEqualByComparingTo("16000.00");
        assertThat(result.status()).isEqualTo("ON_TRACK");
    }

    @Test
    void rejectsInvalidAmountsAndPastCreationDate() {
        assertThatThrownBy(() -> service.create(1L, new FinancialGoalRequest(" ", FinancialGoalType.BIKE,
                new BigDecimal("100"), BigDecimal.ZERO, LocalDate.of(2027, 1, 1), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(1L, new FinancialGoalRequest("Bike", FinancialGoalType.BIKE,
                new BigDecimal("100"), new BigDecimal("101"), LocalDate.of(2027, 1, 1), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(1L, new FinancialGoalRequest("Bike", FinancialGoalType.BIKE,
                new BigDecimal("100"), BigDecimal.ZERO, LocalDate.of(2026, 9, 4), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listUsesUserScopedRepositoryAndReportsCompletedAndNeedsAttention() {
        when(summaryService.summarize(1L, null, null)).thenReturn(summary(80000, 35000));
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        FinancialGoal complete = new FinancialGoal(user, "Trip", FinancialGoalType.TRIP,
                new BigDecimal("100"), new BigDecimal("100"), LocalDate.of(2026, 8, 1), null);
        FinancialGoal attention = new FinancialGoal(user, "House", FinancialGoalType.HOUSE,
                new BigDecimal("1000000"), new BigDecimal("100"), LocalDate.of(2026, 10, 1), null);
        when(goalRepository.findByUserIdOrderByTargetDateAsc(1L)).thenReturn(List.of(complete, attention));

        List<FinancialGoalResponse> result = service.list(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo("COMPLETED");
        assertThat(result.get(1).status()).isEqualTo("NEEDS_ATTENTION");
        verify(goalRepository).findByUserIdOrderByTargetDateAsc(1L);
    }

    @Test
    void getOnlyResolvesGoalThroughAuthenticatedUserScope() {
        when(goalRepository.findByIdAndUserId(44L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(44L, 1L)).isInstanceOf(RuntimeException.class);

        verify(goalRepository).findByIdAndUserId(44L, 1L);
    }

    @Test
    void reportsOverdueGoalWithRemainingAmount() {
        User user = new User("Ada", "ada@example.com", "hash");
        FinancialGoal overdue = new FinancialGoal(user, "Trip", FinancialGoalType.TRIP,
                new BigDecimal("1000"), new BigDecimal("250"), LocalDate.of(2026, 8, 1), null);
        when(summaryService.summarize(1L, null, null)).thenReturn(summary(80000, 35000));
        when(goalRepository.findByUserIdOrderByTargetDateAsc(1L)).thenReturn(List.of(overdue));

        FinancialGoalResponse result = service.list(1L).get(0);

        assertThat(result.status()).isEqualTo("OVERDUE");
        assertThat(result.remainingMonths()).isZero();
        assertThat(result.remainingAmount()).isEqualByComparingTo("750");
    }

    @Test
    void goalProgressCalculatesCorrectPercentage() {
        // Target: 20,00,000, Current: 5,00,000 -> progress = 25.00%
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Car", "CAR",
                new BigDecimal("2000000"), new BigDecimal("500000"),
                LocalDate.of(2028, 9, 4), new BigDecimal("50000"),
                LocalDate.of(2026, 9, 4), "Down payment");

        assertThat(plan.progressPercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    void remainingAmountCalculatesDifferenceCorrectly() {
        // Target: 20,00,000, Current: 5,00,000 -> remaining = 15,00,000.00
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Car", "CAR",
                new BigDecimal("2000000"), new BigDecimal("500000"),
                LocalDate.of(2028, 9, 4), new BigDecimal("50000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(plan.remainingAmount()).isEqualByComparingTo("1500000.00");
    }

    @Test
    void reversePlanningCalculatesRequiredMonthlyContribution() {
        // Target: 20,00,000, Current: 2,00,000, Remaining months: 96
        // Remaining: 18,00,000 -> Required monthly contribution = 18,00,000 / 96 = 18,750.00
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Retirement", "RETIREMENT",
                new BigDecimal("2000000"), new BigDecimal("200000"),
                LocalDate.of(2034, 9, 4), new BigDecimal("25000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(plan.remainingAmount()).isEqualByComparingTo("1800000.00");
        assertThat(plan.remainingMonths()).isEqualTo(96);
        assertThat(plan.requiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(plan.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void alreadyAchievedGoalHasZeroRemainingAndZeroRequiredContribution() {
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Emergency Fund", "EMERGENCY_FUND",
                new BigDecimal("2000000"), new BigDecimal("2000000"),
                LocalDate.of(2028, 9, 4), new BigDecimal("25000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(plan.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(plan.requiredMonthlyContribution()).isEqualByComparingTo("0.00");
        assertThat(plan.progressPercentage()).isEqualByComparingTo("100.00");
        assertThat(plan.status()).isEqualTo("ACHIEVED");
        assertThat(plan.affordable()).isTrue();
        assertThat(plan.shortfallAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void handlesZeroOrInvalidTargetSafelyWithoutException() {
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Zero Goal", "OTHER",
                BigDecimal.ZERO, new BigDecimal("1000"),
                LocalDate.of(2028, 9, 4), new BigDecimal("25000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(plan.progressPercentage()).isEqualByComparingTo("0.00");
        assertThat(plan.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(plan.requiredMonthlyContribution()).isEqualByComparingTo("0.00");
    }

    @Test
    void pastTargetDateHandlesOverdueGoalWithoutDivisionByZero() {
        // Target date in the past -> remainingMonths = 0, no divide by zero, status = OVERDUE
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Past Vacation", "VACATION",
                new BigDecimal("50000"), new BigDecimal("10000"),
                LocalDate.of(2026, 8, 1), new BigDecimal("20000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(plan.remainingMonths()).isZero();
        assertThat(plan.remainingAmount()).isEqualByComparingTo("40000.00");
        assertThat(plan.requiredMonthlyContribution()).isEqualByComparingTo("0.00");
        assertThat(plan.status()).isEqualTo("OVERDUE");
    }

    @Test
    void affordabilityAnalysisDeterminesAffordableAndShortfall() {
        // Case 1: Required 18,750 <= Disposable 25,000 -> affordable = true, shortfall = 0
        GoalPlanResponse affordablePlan = service.calculatePlan(
                1L, "Goal", "OTHER",
                new BigDecimal("2000000"), new BigDecimal("200000"),
                LocalDate.of(2034, 9, 4), new BigDecimal("25000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(affordablePlan.requiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(affordablePlan.disposableIncome()).isEqualByComparingTo("25000.00");
        assertThat(affordablePlan.affordable()).isTrue();
        assertThat(affordablePlan.shortfallAmount()).isEqualByComparingTo("0.00");

        // Case 2: Required 18,750 > Disposable 10,000 -> affordable = false, shortfall = 8,750
        GoalPlanResponse unaffordablePlan = service.calculatePlan(
                1L, "Goal", "OTHER",
                new BigDecimal("2000000"), new BigDecimal("200000"),
                LocalDate.of(2034, 9, 4), new BigDecimal("10000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(unaffordablePlan.requiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(unaffordablePlan.disposableIncome()).isEqualByComparingTo("10000.00");
        assertThat(unaffordablePlan.affordable()).isFalse();
        assertThat(unaffordablePlan.shortfallAmount()).isEqualByComparingTo("8750.00");
    }

    @Test
    void userCannotAccessAnotherUsersGoalForPlanning() {
        when(goalRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPlan(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(goalRepository).findByIdAndUserId(99L, 1L);
    }

    @Test
    void bigDecimalPrecisionAndRoundingHandledCorrectly() {
        // Target: 1,00,000, Current: 0, 3 months remaining -> 100,000 / 3 = 33,333.33
        GoalPlanResponse plan = service.calculatePlan(
                1L, "Trip", "TRIP",
                new BigDecimal("100000"), BigDecimal.ZERO,
                LocalDate.of(2026, 12, 4), new BigDecimal("50000"),
                LocalDate.of(2026, 9, 4), null);

        assertThat(plan.requiredMonthlyContribution()).isEqualByComparingTo("33333.33");
        assertThat(plan.progressPercentage()).isEqualByComparingTo("0.00");
        assertThat(plan.status()).isEqualTo("NOT_STARTED");
    }

    @Test
    void getPlanOrchestratesRepositoryAndSummaryService() {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        FinancialGoal goal = new FinancialGoal(user, "Home", FinancialGoalType.HOUSE,
                new BigDecimal("2000000"), new BigDecimal("200000"), LocalDate.of(2034, 9, 4), "Dream home");
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal));
        when(summaryService.summarize(1L, null, null)).thenReturn(
                new FinancialSummaryResponse(null, null, new BigDecimal("100000"), new BigDecimal("70000"),
                        new BigDecimal("30000"), new BigDecimal("30.00"),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));

        GoalPlanResponse response = service.getPlan(10L, 1L);

        assertThat(response.goalId()).isEqualTo(goal.getId());
        assertThat(response.goalName()).isEqualTo("Home");
        assertThat(response.targetAmount()).isEqualByComparingTo("2000000.00");
        assertThat(response.currentAmount()).isEqualByComparingTo("200000.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("1800000.00");
        assertThat(response.remainingMonths()).isEqualTo(96);
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("30000.00");
        assertThat(response.affordable()).isTrue();
        assertThat(response.shortfallAmount()).isEqualByComparingTo("0.00");
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void achievedGoalAnalysis() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                1L, "Bike", "BIKE",
                new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                LocalDate.of(2027, 9, 4), new BigDecimal("100.00"),
                BigDecimal.ZERO, 12, BigDecimal.ZERO,
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "ACHIEVED", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("6000.00"), new BigDecimal("20000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("ACHIEVED");
        assertThat(response.achievable()).isTrue();
        assertThat(response.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("100.00");
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("0.00");
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("6000.00");
        assertThat(response.projectedAmountAtTarget()).isEqualByComparingTo("100000.00");
        assertThat(response.analysisMessage()).contains("achieved");
    }

    @Test
    void onTrackGoalAnalysis() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                2L, "Trip", "TRIP",
                new BigDecimal("200000.00"), new BigDecimal("50000.00"),
                LocalDate.of(2029, 3, 4), new BigDecimal("25.00"),
                new BigDecimal("150000.00"), 30, new BigDecimal("5000.00"),
                new BigDecimal("30000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("9000.00"), new BigDecimal("30000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("ON_TRACK");
        assertThat(response.achievable()).isTrue();
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("5000.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("9000.00");
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("4000.00");
        // Projected amount should include initial 50,000 + SIP future value of 9,000/mo over 30 months
        assertThat(response.projectedAmountAtTarget()).isGreaterThan(new BigDecimal("320000.00"));
        assertThat(response.analysisMessage()).contains("on track");
    }

    @Test
    void atRiskGoalAnalysis() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                3L, "House", "HOUSE",
                new BigDecimal("500000.00"), new BigDecimal("50000.00"),
                LocalDate.of(2028, 9, 4), new BigDecimal("10.00"),
                new BigDecimal("450000.00"), 24, new BigDecimal("18750.00"),
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("6000.00"), new BigDecimal("20000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("AT_RISK");
        assertThat(response.achievable()).isFalse();
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6000.00");
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("12750.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("0.00");
        assertThat(response.analysisMessage()).contains("at risk");
        assertThat(response.warnings()).anyMatch(w -> w.contains("Monthly shortfall"));
    }

    @Test
    void notFeasibleGoalZeroDisposableIncome() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                4L, "Car", "CAR",
                new BigDecimal("200000.00"), BigDecimal.ZERO,
                LocalDate.of(2028, 5, 4), BigDecimal.ZERO,
                new BigDecimal("200000.00"), 20, new BigDecimal("10000.00"),
                BigDecimal.ZERO, false, new BigDecimal("10000.00"),
                "NOT_STARTED", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                BigDecimal.ZERO, BigDecimal.ZERO, InvestmentRiskPreference.CONSERVATIVE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("NOT_FEASIBLE");
        assertThat(response.achievable()).isFalse();
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("10000.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("0.00");
        assertThat(response.projectedAmountAtTarget()).isEqualByComparingTo("0.00");
        assertThat(response.analysisMessage()).contains("not feasible");
    }

    @Test
    void notFeasibleGoalNegativeDisposableIncome() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                5L, "Car", "CAR",
                new BigDecimal("200000.00"), new BigDecimal("10000.00"),
                LocalDate.of(2028, 4, 4), new BigDecimal("5.00"),
                new BigDecimal("190000.00"), 19, new BigDecimal("10000.00"),
                new BigDecimal("-5000.00"), false, new BigDecimal("15000.00"),
                "IN_PROGRESS", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                BigDecimal.ZERO, new BigDecimal("-5000.00"), InvestmentRiskPreference.CONSERVATIVE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("NOT_FEASIBLE");
        assertThat(response.achievable()).isFalse();
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("10000.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("0.00");
        assertThat(response.projectedAmountAtTarget()).isEqualByComparingTo("10000.00");
        assertThat(response.analysisMessage()).contains("not feasible");
    }

    @Test
    void overdueGoalAnalysis() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                6L, "Gadget", "CUSTOM",
                new BigDecimal("50000.00"), new BigDecimal("20000.00"),
                LocalDate.of(2026, 8, 1), new BigDecimal("40.00"),
                new BigDecimal("30000.00"), 0, BigDecimal.ZERO,
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "OVERDUE", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("6000.00"), new BigDecimal("20000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("OVERDUE");
        assertThat(response.achievable()).isFalse();
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("0.00");
        assertThat(response.projectedAmountAtTarget()).isEqualByComparingTo("20000.00");
        assertThat(response.analysisMessage()).contains("overdue");
        assertThat(response.warnings()).anyMatch(w -> w.contains("passed"));
    }

    @Test
    void targetDateEqualCurrentDateOverdueAnalysis() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                7L, "Camera", "CUSTOM",
                new BigDecimal("60000.00"), new BigDecimal("25000.00"),
                LocalDate.of(2026, 9, 4), new BigDecimal("41.67"),
                new BigDecimal("35000.00"), 0, BigDecimal.ZERO,
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "OVERDUE", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("6000.00"), new BigDecimal("20000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.achievementStatus()).isEqualTo("OVERDUE");
        assertThat(response.achievable()).isFalse();
        assertThat(response.projectedAmountAtTarget()).isEqualByComparingTo("25000.00");
    }

    @Test
    void correctRemainingAmountAndProgressPercentage() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                8L, "Vacation", "TRIP",
                new BigDecimal("300000.00"), new BigDecimal("75000.00"),
                LocalDate.of(2028, 9, 4), new BigDecimal("25.00"),
                new BigDecimal("225000.00"), 24, new BigDecimal("9375.00"),
                new BigDecimal("25000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("10000.00"), new BigDecimal("25000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.targetAmount()).isEqualByComparingTo("300000.00");
        assertThat(response.currentAmount()).isEqualByComparingTo("75000.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("225000.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("25.00");
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("9375.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("10000.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("625.00");
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(response.achievementStatus()).isEqualTo("ON_TRACK");
        assertThat(response.achievable()).isTrue();
    }

    @Test
    void projectedAmountAtTargetWithCompounding() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                9L, "Education", "EDUCATION",
                new BigDecimal("200000.00"), new BigDecimal("20000.00"),
                LocalDate.of(2027, 9, 4), new BigDecimal("10.00"),
                new BigDecimal("180000.00"), 12, new BigDecimal("15000.00"),
                new BigDecimal("35000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        // Moderate: 10% expected return. Monthly investment 10,000 for 12 months.
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("10000.00"), new BigDecimal("35000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        // FV(10000, 10%, 12) = 126,702.81, plus initial 20,000 = 146,702.81
        assertThat(response.projectedAmountAtTarget()).isEqualByComparingTo("146702.81");
    }

    @Test
    void verySmallRemainingAmountPrecision() {
        LocalDate today = LocalDate.of(2026, 9, 4);
        GoalPlanResponse plan = new GoalPlanResponse(
                10L, "Close Goal", "CUSTOM",
                new BigDecimal("100000.00"), new BigDecimal("99999.50"),
                LocalDate.of(2026, 10, 4), new BigDecimal("100.00"),
                new BigDecimal("0.50"), 1, new BigDecimal("0.50"),
                new BigDecimal("10000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("10.00"), new BigDecimal("10000.00"), InvestmentRiskPreference.MODERATE);

        GoalAchievementResponse response = service.calculateAchievement(plan, rec, today);

        assertThat(response.remainingAmount()).isEqualByComparingTo("0.50");
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("0.50");
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("0.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("9.50");
        assertThat(response.achievementStatus()).isEqualTo("ON_TRACK");
        assertThat(response.achievable()).isTrue();
    }

    @Test
    void userIsolationInGetAchievement() {
        when(goalRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAchievement(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(goalRepository).findByIdAndUserId(99L, 1L);
    }

    @Test
    void getAchievementOrchestratesPlanAndRecommendation() {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        FinancialGoal goal = new FinancialGoal(user, "Home", FinancialGoalType.HOUSE,
                new BigDecimal("2000000"), new BigDecimal("200000"), LocalDate.of(2034, 9, 4), "Dream home");
        when(goalRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(goal));
        when(summaryService.summarize(1L, null, null)).thenReturn(
                new FinancialSummaryResponse(null, null, new BigDecimal("100000"), new BigDecimal("70000"),
                        new BigDecimal("30000"), new BigDecimal("30.00"),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));

        InvestmentRecommendationResponse rec = mockRecommendation(
                new BigDecimal("9000.00"), new BigDecimal("30000.00"), InvestmentRiskPreference.MODERATE);
        when(investmentRecommendationService.recommend(1L, 10L)).thenReturn(rec);

        GoalAchievementResponse response = service.getAchievement(10L, 1L);

        assertThat(response.goalId()).isEqualTo(goal.getId());
        assertThat(response.goalName()).isEqualTo("Home");
        assertThat(response.targetAmount()).isEqualByComparingTo("2000000.00");
        assertThat(response.currentAmount()).isEqualByComparingTo("200000.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("1800000.00");
        assertThat(response.remainingMonths()).isEqualTo(96);
        assertThat(response.requiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("9000.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("30000.00");
        assertThat(response.monthlyShortfall()).isEqualByComparingTo("9750.00");
        assertThat(response.monthlySurplus()).isEqualByComparingTo("0.00");
        assertThat(response.achievementStatus()).isEqualTo("AT_RISK");
        assertThat(response.achievable()).isFalse();
    }

    private InvestmentRecommendationResponse mockRecommendation(
            BigDecimal recommendedMonthlyInvestment,
            BigDecimal disposableIncome,
            InvestmentRiskPreference riskPreference) {
        return new InvestmentRecommendationResponse(
                recommendedMonthlyInvestment,
                disposableIncome,
                new BigDecimal("30.00"),
                true,
                BigDecimal.ZERO,
                riskPreference,
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                "DIVERSIFIED",
                "Diversified portfolio",
                null, null, null, null, null, null,
                "Reason",
                List.of());
    }

    private FinancialSummaryResponse summary(int income, int expenses) {
        return new FinancialSummaryResponse(null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME,
                                BigDecimal.valueOf(income)),
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE,
                                BigDecimal.valueOf(expenses))));
    }
}