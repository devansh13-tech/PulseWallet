package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.GoalPlanResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentProjectionResponse;
import com.pulsewallet.pulsewallet.dto.InvestmentRecommendationResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.FinancialGoalType;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class InvestmentRecommendationServiceTest {

    @Mock
    private FinancialSummaryService financialSummaryService;
    @Mock
    private EmergencyFundService emergencyFundService;
    @Mock
    private FinancialGoalService financialGoalService;
    @Mock
    private FinancialGoalRepository financialGoalRepository;
    @Mock
    private InvestmentProfileRepository investmentProfileRepository;

    private InvestmentRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentRecommendationService(
                financialSummaryService,
                emergencyFundService,
                financialGoalService,
                financialGoalRepository,
                investmentProfileRepository);
    }

    @Test
    void positiveDisposableIncomeRecommendsThirtyPercent() {
        // Disposable = 20,000 -> 30% = 6,000
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("20000.00");
        assertThat(response.investmentAllocationPercentage()).isEqualByComparingTo("30.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6000.00");
        assertThat(response.emergencyFundReady()).isTrue();
        assertThat(response.riskPreference()).isEqualTo(InvestmentRiskPreference.MODERATE);
    }

    @Test
    void zeroDisposableIncomeProducesZeroSIP() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(BigDecimal.ZERO));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.empty());
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("0.00");
        assertThat(response.equityAmount()).isEqualByComparingTo("0.00");
        assertThat(response.debtAmount()).isEqualByComparingTo("0.00");
        assertThat(response.goldAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void negativeDisposableIncomeProducesZeroSIPAndNoNegativeValues() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("-15000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.empty());
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("-15000.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("0.00");
        assertThat(response.equityAmount()).isEqualByComparingTo("0.00");
        assertThat(response.debtAmount()).isEqualByComparingTo("0.00");
        assertThat(response.goldAmount()).isEqualByComparingTo("0.00");
        assertThat(response.warnings()).anyMatch(w -> w.contains("zero or negative"));
    }

    @Test
    void conservativeAllocationCalculatesThirtyFiftyTwenty() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.CONSERVATIVE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        // SIP = 6,000 -> 30% Equity = 1800, 50% Debt = 3000, 20% Gold = 1200
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6000.00");
        assertThat(response.equityPercentage()).isEqualByComparingTo("30.00");
        assertThat(response.debtPercentage()).isEqualByComparingTo("50.00");
        assertThat(response.goldPercentage()).isEqualByComparingTo("20.00");
        assertThat(response.equityPercentage().add(response.debtPercentage()).add(response.goldPercentage()))
                .isEqualByComparingTo("100.00");
        assertThat(response.equityAmount()).isEqualByComparingTo("1800.00");
        assertThat(response.debtAmount()).isEqualByComparingTo("3000.00");
        assertThat(response.goldAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.equityAmount().add(response.debtAmount()).add(response.goldAmount()))
                .isEqualByComparingTo(response.recommendedMonthlyInvestment());
        assertThat(response.diversified()).isTrue();
        assertThat(response.diversificationStatus()).isEqualTo("DIVERSIFIED");
        assertThat(response.diversificationMessage()).contains("lower volatility");
    }

    @Test
    void moderateAllocationCalculatesSixtyThirtyTen() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        // SIP = 6,000 -> 60% Equity = 3600, 30% Debt = 1800, 10% Gold = 600
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6000.00");
        assertThat(response.equityPercentage()).isEqualByComparingTo("60.00");
        assertThat(response.debtPercentage()).isEqualByComparingTo("30.00");
        assertThat(response.goldPercentage()).isEqualByComparingTo("10.00");
        assertThat(response.equityPercentage().add(response.debtPercentage()).add(response.goldPercentage()))
                .isEqualByComparingTo("100.00");
        assertThat(response.equityAmount()).isEqualByComparingTo("3600.00");
        assertThat(response.debtAmount()).isEqualByComparingTo("1800.00");
        assertThat(response.goldAmount()).isEqualByComparingTo("600.00");
        assertThat(response.equityAmount().add(response.debtAmount()).add(response.goldAmount()))
                .isEqualByComparingTo(response.recommendedMonthlyInvestment());
        assertThat(response.diversified()).isTrue();
        assertThat(response.diversificationStatus()).isEqualTo("DIVERSIFIED");
        assertThat(response.diversificationMessage()).contains("balanced growth and stability");
    }

    @Test
    void aggressiveAllocationCalculatesEightyTenTen() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.AGGRESSIVE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        // SIP = 6,000 -> 80% Equity = 4800, 10% Debt = 600, 10% Gold = 600
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6000.00");
        assertThat(response.equityPercentage()).isEqualByComparingTo("80.00");
        assertThat(response.debtPercentage()).isEqualByComparingTo("10.00");
        assertThat(response.goldPercentage()).isEqualByComparingTo("10.00");
        assertThat(response.equityPercentage().add(response.debtPercentage()).add(response.goldPercentage()))
                .isEqualByComparingTo("100.00");
        assertThat(response.equityAmount()).isEqualByComparingTo("4800.00");
        assertThat(response.debtAmount()).isEqualByComparingTo("600.00");
        assertThat(response.goldAmount()).isEqualByComparingTo("600.00");
        assertThat(response.equityAmount().add(response.debtAmount()).add(response.goldAmount()))
                .isEqualByComparingTo(response.recommendedMonthlyInvestment());
        assertThat(response.diversified()).isTrue();
        assertThat(response.diversificationStatus()).isEqualTo("DIVERSIFIED");
        assertThat(response.diversificationMessage()).contains("higher long-term growth potential");
    }

    @Test
    void allocationAlwaysSumsToExactlyOneHundredPercent() {
        for (InvestmentRiskPreference risk : InvestmentRiskPreference.values()) {
            when(financialSummaryService.summarize(1L, null, null))
                    .thenReturn(mockSummary(new BigDecimal("10000.00")));
            when(emergencyFundService.calculate(1L))
                    .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
            when(investmentProfileRepository.findByUserId(1L))
                    .thenReturn(Optional.of(mockProfile(risk)));
            when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                    .thenReturn(List.of());

            InvestmentRecommendationResponse response = service.recommend(1L);

            BigDecimal totalPercentage = response.equityPercentage()
                    .add(response.debtPercentage())
                    .add(response.goldPercentage());
            assertThat(totalPercentage).isEqualByComparingTo("100.00");
            assertThat(response.diversified()).isTrue();
            assertThat(response.diversificationStatus()).isEqualTo("DIVERSIFIED");
        }
    }

    @Test
    void assessDiversificationStatusClassifiesAssetSpreadCorrectly() {
        // 3 asset classes -> DIVERSIFIED
        assertThat(InvestmentRecommendationService.assessDiversificationStatus(
                new BigDecimal("60"), new BigDecimal("30"), new BigDecimal("10")))
                .isEqualTo("DIVERSIFIED");

        // 2 asset classes -> PARTIALLY_DIVERSIFIED
        assertThat(InvestmentRecommendationService.assessDiversificationStatus(
                new BigDecimal("70"), new BigDecimal("30"), BigDecimal.ZERO))
                .isEqualTo("PARTIALLY_DIVERSIFIED");

        // 1 asset class -> NOT_DIVERSIFIED
        assertThat(InvestmentRecommendationService.assessDiversificationStatus(
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualTo("NOT_DIVERSIFIED");

        // 0 asset classes -> NOT_DIVERSIFIED
        assertThat(InvestmentRecommendationService.assessDiversificationStatus(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualTo("NOT_DIVERSIFIED");
    }

    @Test
    void diversificationMessageWarnsWhenNotDiversified() {
        String msg = InvestmentRecommendationService.buildDiversificationMessage(
                InvestmentRiskPreference.MODERATE,
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                "NOT_DIVERSIFIED");

        assertThat(msg).contains("lacks multi-asset diversification")
                .contains("concentration risk");
    }

    @Test
    void allocationAmountsSumExactlyToRecommendedSIPIrrespectiveOfRounding() {
        // Disposable = 333.37 -> 30% = 100.01
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("333.37")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.CONSERVATIVE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("100.01");
        BigDecimal sum = response.equityAmount().add(response.debtAmount()).add(response.goldAmount());
        assertThat(sum).isEqualByComparingTo(response.recommendedMonthlyInvestment());
    }

    @Test
    void emergencyFundIncompleteReducesAllocationToFifteenPercent() {
        // Disposable = 20,000, Emergency fund needs 25,000 -> allocation reduced to 15% = 3,000
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(new BigDecimal("25000.00")));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.emergencyFundReady()).isFalse();
        assertThat(response.emergencyFundRemaining()).isEqualByComparingTo("25000.00");
        assertThat(response.investmentAllocationPercentage()).isEqualByComparingTo("15.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("3000.00");
        assertThat(response.warnings()).anyMatch(w -> w.contains("Emergency fund is not fully funded"));
    }

    @Test
    void emergencyFundReadyAppliesNormalThirtyPercentAllocation() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.emergencyFundReady()).isTrue();
        assertThat(response.emergencyFundRemaining()).isEqualByComparingTo("0.00");
        assertThat(response.investmentAllocationPercentage()).isEqualByComparingTo("30.00");
        assertThat(response.recommendedMonthlyInvestment()).isEqualByComparingTo("6000.00");
    }

    @Test
    void goalAwareRecommendationComparesCapacityAndRequiredContribution() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));

        // Case 1: Required contribution = 4,000, Capacity = 6,000 -> covered = true
        GoalPlanResponse coveredGoalPlan = new GoalPlanResponse(
                5L, "Car Downpayment", "CAR",
                new BigDecimal("200000.00"), new BigDecimal("50000.00"),
                LocalDate.of(2028, 1, 1), new BigDecimal("25.00"),
                new BigDecimal("150000.00"), 36, new BigDecimal("4000.00"),
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", "Car savings");
        when(financialGoalService.getPlan(5L, 1L)).thenReturn(coveredGoalPlan);

        InvestmentRecommendationResponse coveredResponse = service.recommend(1L, 5L);

        assertThat(coveredResponse.goalName()).isEqualTo("Car Downpayment");
        assertThat(coveredResponse.goalRequiredMonthlyContribution()).isEqualByComparingTo("4000.00");
        assertThat(coveredResponse.goalContributionCovered()).isTrue();
        assertThat(coveredResponse.recommendationReason()).contains("is sufficient to cover");

        // Case 2: Required contribution = 18,750, Capacity = 6,000 -> covered = false
        GoalPlanResponse uncoveredGoalPlan = new GoalPlanResponse(
                6L, "House", "HOUSE",
                new BigDecimal("2000000.00"), new BigDecimal("200000.00"),
                LocalDate.of(2034, 1, 1), new BigDecimal("10.00"),
                new BigDecimal("1800000.00"), 96, new BigDecimal("18750.00"),
                new BigDecimal("20000.00"), false, new BigDecimal("12750.00"),
                "IN_PROGRESS", "House savings");
        when(financialGoalService.getPlan(6L, 1L)).thenReturn(uncoveredGoalPlan);

        InvestmentRecommendationResponse uncoveredResponse = service.recommend(1L, 6L);

        assertThat(uncoveredResponse.goalName()).isEqualTo("House");
        assertThat(uncoveredResponse.goalRequiredMonthlyContribution()).isEqualByComparingTo("18750.00");
        assertThat(uncoveredResponse.goalContributionCovered()).isFalse();
        assertThat(uncoveredResponse.recommendationReason()).contains("is below the required monthly contribution");
        assertThat(uncoveredResponse.warnings()).anyMatch(w -> w.contains("does not fully cover the goal requirement"));
    }

    @Test
    void userIsolationRejectsAccessToAnotherUsersGoal() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(financialGoalService.getPlan(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Financial goal", 99L));

        assertThatThrownBy(() -> service.recommend(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(financialGoalService).getPlan(99L, 1L);
    }

    @Test
    void noActiveGoalReturnsGeneralRecommendationWithNullGoalFields() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentRecommendationResponse response = service.recommend(1L);

        assertThat(response.goalName()).isNull();
        assertThat(response.goalTargetAmount()).isNull();
        assertThat(response.goalCurrentAmount()).isNull();
        assertThat(response.goalRemainingAmount()).isNull();
        assertThat(response.goalRequiredMonthlyContribution()).isNull();
        assertThat(response.goalContributionCovered()).isNull();
        assertThat(response.recommendationReason()).contains("General wealth accumulation recommendation");
    }

    @Test
    void autoDiscoversFirstActiveUncompletedGoalWhenGoalIdIsNull() {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        FinancialGoal activeGoal = TestEntities.withId(
                new FinancialGoal(user, "Wedding", FinancialGoalType.CUSTOM,
                        new BigDecimal("500000.00"), new BigDecimal("100000.00"), LocalDate.of(2028, 1, 1), null),
                12L);

        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of(activeGoal));

        GoalPlanResponse autoGoalPlan = new GoalPlanResponse(
                12L, "Wedding", "WEDDING",
                new BigDecimal("500000.00"), new BigDecimal("100000.00"),
                LocalDate.of(2028, 1, 1), new BigDecimal("20.00"),
                new BigDecimal("400000.00"), 24, new BigDecimal("16666.67"),
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        when(financialGoalService.getPlan(12L, 1L)).thenReturn(autoGoalPlan);

        InvestmentRecommendationResponse response = service.recommend(1L, null);

        assertThat(response.goalName()).isEqualTo("Wedding");
        assertThat(response.goalRequiredMonthlyContribution()).isEqualByComparingTo("16666.67");
    }

    @Test
    void moderateTenYearProjectionCalculatesExpectedGrowthAndFutureValue() {
        // Disposable = 20,000 -> SIP = 6,000, 10 years = 120 months, Moderate return = 10%
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));
        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        InvestmentProjectionResponse response = service.project(1L, null, 10);

        assertThat(response.monthlyInvestment()).isEqualByComparingTo("6000.00");
        assertThat(response.years()).isEqualTo(10);
        assertThat(response.months()).isEqualTo(120);
        assertThat(response.annualExpectedReturnPercentage()).isEqualByComparingTo("10.00");
        assertThat(response.monthlyExpectedReturnPercentage()).isEqualByComparingTo("0.8333");
        assertThat(response.totalContributed()).isEqualByComparingTo("720000.00"); // 6000 * 120
        assertThat(response.projectedFutureValue()).isGreaterThan(response.totalContributed());
        assertThat(response.estimatedGrowth()).isEqualByComparingTo(
                response.projectedFutureValue().subtract(response.totalContributed()));
        assertThat(response.riskPreference()).isEqualTo(InvestmentRiskPreference.MODERATE);
        assertThat(response.projectionMessage()).contains("Returns are illustrative");
    }

    @Test
    void riskPreferenceDeterminesAnnualExpectedReturn() {
        assertThat(InvestmentRecommendationService.expectedReturnFor(InvestmentRiskPreference.CONSERVATIVE))
                .isEqualByComparingTo("7.00");
        assertThat(InvestmentRecommendationService.expectedReturnFor(InvestmentRiskPreference.MODERATE))
                .isEqualByComparingTo("10.00");
        assertThat(InvestmentRecommendationService.expectedReturnFor(InvestmentRiskPreference.AGGRESSIVE))
                .isEqualByComparingTo("12.00");
        assertThat(InvestmentRecommendationService.expectedReturnFor(null))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void futureValueHandlesZeroMonthlyInvestment() {
        BigDecimal fv = InvestmentRecommendationService.calculateFutureValue(BigDecimal.ZERO, new BigDecimal("10.00"), 120);
        assertThat(fv).isEqualByComparingTo("0.00");

        BigDecimal fvNeg = InvestmentRecommendationService.calculateFutureValue(new BigDecimal("-500.00"), new BigDecimal("10.00"), 120);
        assertThat(fvNeg).isEqualByComparingTo("0.00");
    }

    @Test
    void futureValueHandlesZeroExpectedReturn() {
        // When r = 0, FV = P * n = 5000 * 120 = 600,000
        BigDecimal fv = InvestmentRecommendationService.calculateFutureValue(
                new BigDecimal("5000.00"), BigDecimal.ZERO, 120);
        assertThat(fv).isEqualByComparingTo("600000.00");
    }

    @Test
    void totalContributionAndEstimatedGrowthCalculatedCorrectly() {
        // P = 10,000, n = 60 (5 years), r = 12%
        BigDecimal p = new BigDecimal("10000.00");
        BigDecimal fv = InvestmentRecommendationService.calculateFutureValue(p, new BigDecimal("12.00"), 60);
        BigDecimal totalContributed = p.multiply(BigDecimal.valueOf(60));
        BigDecimal estimatedGrowth = fv.subtract(totalContributed);

        assertThat(totalContributed).isEqualByComparingTo("600000.00");
        assertThat(fv).isGreaterThan(totalContributed);
        assertThat(estimatedGrowth).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void goalAwareProjectionComparesProjectedValueAgainstGoalRemainingAmount() {
        when(financialSummaryService.summarize(1L, null, null))
                .thenReturn(mockSummary(new BigDecimal("20000.00")));
        when(emergencyFundService.calculate(1L))
                .thenReturn(mockEmergencyFund(BigDecimal.ZERO));
        when(investmentProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockProfile(InvestmentRiskPreference.MODERATE)));

        // Case 1: FV will be ~12-13 lakhs over 10 years, remaining is 5 lakhs -> covered = true
        GoalPlanResponse coveredGoal = new GoalPlanResponse(
                5L, "Bike", "BIKE",
                new BigDecimal("600000.00"), new BigDecimal("100000.00"),
                LocalDate.of(2036, 1, 1), new BigDecimal("16.67"),
                new BigDecimal("500000.00"), 120, new BigDecimal("4166.67"),
                new BigDecimal("20000.00"), true, BigDecimal.ZERO,
                "IN_PROGRESS", null);
        when(financialGoalService.getPlan(5L, 1L)).thenReturn(coveredGoal);

        InvestmentProjectionResponse coveredResponse = service.project(1L, 5L, 10);

        assertThat(coveredResponse.goalName()).isEqualTo("Bike");
        assertThat(coveredResponse.goalRemainingAmount()).isEqualByComparingTo("500000.00");
        assertThat(coveredResponse.goalProjectedValueCovered()).isTrue();
        assertThat(coveredResponse.projectionMessage()).contains("is projected to cover your remaining goal amount");

        // Case 2: Remaining is 50 lakhs -> covered = false
        GoalPlanResponse uncoveredGoal = new GoalPlanResponse(
                6L, "Mansion", "HOUSE",
                new BigDecimal("6000000.00"), new BigDecimal("1000000.00"),
                LocalDate.of(2036, 1, 1), new BigDecimal("16.67"),
                new BigDecimal("5000000.00"), 120, new BigDecimal("41666.67"),
                new BigDecimal("20000.00"), false, new BigDecimal("35666.67"),
                "IN_PROGRESS", null);
        when(financialGoalService.getPlan(6L, 1L)).thenReturn(uncoveredGoal);

        InvestmentProjectionResponse uncoveredResponse = service.project(1L, 6L, 10);

        assertThat(uncoveredResponse.goalName()).isEqualTo("Mansion");
        assertThat(uncoveredResponse.goalRemainingAmount()).isEqualByComparingTo("5000000.00");
        assertThat(uncoveredResponse.goalProjectedValueCovered()).isFalse();
        assertThat(uncoveredResponse.projectionMessage()).contains("is below your remaining goal amount");
        assertThat(uncoveredResponse.warnings()).anyMatch(w -> w.contains("Projected future value does not cover"));
    }

    @Test
    void projectValidatesYearsParameter() {
        assertThatThrownBy(() -> service.project(1L, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("years must be between 1 and 50");

        assertThatThrownBy(() -> service.project(1L, null, -5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.project(1L, null, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FinancialSummaryResponse mockSummary(BigDecimal disposable) {
        return new FinancialSummaryResponse(null, null, disposable.max(BigDecimal.ZERO), BigDecimal.ZERO, disposable,
                BigDecimal.ZERO, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private EmergencyFundPlanResponse mockEmergencyFund(BigDecimal remainingNeeded) {
        return new EmergencyFundPlanResponse(null, null, new BigDecimal("10000"), 6, new BigDecimal("60000"),
                new BigDecimal("60000").subtract(remainingNeeded).max(BigDecimal.ZERO),
                remainingNeeded, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    private InvestmentProfile mockProfile(InvestmentRiskPreference risk) {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), 1L);
        return new InvestmentProfile(user, risk, 5);
    }
}
