package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.AmountByCategory;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.FinancialGoalType;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class EmergencyFundServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinancialSummaryService financialSummaryService;

    @Mock
    private FinancialGoalRepository financialGoalRepository;

    @Mock
    private InvestmentProfileRepository investmentProfileRepository;

    private EmergencyFundService emergencyFundService;

    @BeforeEach
    void setUp() {
        emergencyFundService = new EmergencyFundService(
                financialSummaryService,
                financialGoalRepository,
                investmentProfileRepository);
    }

    private FinancialSummaryResponse createSummaryWithExpenses(
            List<AmountByCategory> expenses, BigDecimal disposableIncome) {
        return new FinancialSummaryResponse(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("80000.00"),
                new BigDecimal("60000.00"),
                disposableIncome,
                new BigDecimal("25.00"),
                List.of(),
                List.of(),
                expenses,
                List.of(),
                List.of(),
                List.of());
    }

    @Test
    void calculate_essentialExpenseCalculation_includesOnlyEssentialCategories() {
        // 1. Essential expense calculation
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("20000.00")),
                new AmountByCategory(2L, "Utilities", new BigDecimal("5000.00")),
                new AmountByCategory(3L, "Groceries", new BigDecimal("10000.00")),
                new AmountByCategory(4L, "Healthcare", new BigDecimal("3000.00")),
                new AmountByCategory(5L, "Education", new BigDecimal("4000.00")),
                new AmountByCategory(6L, "Transportation", new BigDecimal("2000.00")),
                // Discretionary categories
                new AmountByCategory(7L, "Entertainment", new BigDecimal("6000.00")),
                new AmountByCategory(8L, "Shopping", new BigDecimal("8000.00")),
                new AmountByCategory(9L, "Food & Dining", new BigDecimal("7000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("20000.00")));

        EmergencyFundPlanResponse response = emergencyFundService.calculate(USER_ID);

        // 20000 + 5000 + 10000 + 3000 + 4000 + 2000 = 44000
        assertThat(response.monthlyEssentialExpenses()).isEqualByComparingTo("44000.00");
        assertThat(response.essentialCategories()).hasSize(6);
    }

    @Test
    void calculate_sixMonthEmergencyFundTarget_calculatedByDefault() {
        // 2. 6-month emergency fund target
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("15000.00")),
                new AmountByCategory(2L, "Groceries", new BigDecimal("5000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("10000.00")));

        EmergencyFundPlanResponse response = emergencyFundService.calculate(USER_ID);

        assertThat(response.monthlyEssentialExpenses()).isEqualByComparingTo("20000.00");
        assertThat(response.recommendedMonths()).isEqualTo(6);
        // 20000 * 6 = 120000
        assertThat(response.recommendedEmergencyFundTarget()).isEqualByComparingTo("120000.00");
    }

    @Test
    void calculate_currentFundAlreadyAboveTarget_resultsInZeroRemainingAndZeroContribution() {
        // 3. current fund already above target
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("10000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("10000.00")));

        // Target: 60,000. Current: 80,000
        EmergencyFundPlanResponse response = emergencyFundService.calculate(
                USER_ID, null, null, new BigDecimal("80000.00"), null);

        assertThat(response.recommendedEmergencyFundTarget()).isEqualByComparingTo("60000.00");
        assertThat(response.currentEmergencyFund()).isEqualByComparingTo("80000.00");
        assertThat(response.remainingAmountNeeded()).isEqualByComparingTo("0.00");
        assertThat(response.suggestedMonthlyContribution()).isEqualByComparingTo("0.00");
        assertThat(response.progressPercentage()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void calculate_remainingAmountCalculation_correctWhenCurrentFundBelowTarget() {
        // 4. remaining amount calculation
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("10000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("10000.00")));

        // Target: 60,000. Current: 25,000
        EmergencyFundPlanResponse response = emergencyFundService.calculate(
                USER_ID, null, null, new BigDecimal("25000.00"), null);

        assertThat(response.recommendedEmergencyFundTarget()).isEqualByComparingTo("60000.00");
        assertThat(response.currentEmergencyFund()).isEqualByComparingTo("25000.00");
        assertThat(response.remainingAmountNeeded()).isEqualByComparingTo("35000.00");
        // 20% of disposable 10,000 = 2000
        assertThat(response.suggestedMonthlyContribution()).isEqualByComparingTo("2000.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("41.67");
    }

    @Test
    void calculate_zeroEssentialExpenses_resultsInZeroTarget() {
        // 5. zero essential expenses
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Entertainment", new BigDecimal("5000.00")),
                new AmountByCategory(2L, "Shopping", new BigDecimal("4000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("10000.00")));

        EmergencyFundPlanResponse response = emergencyFundService.calculate(USER_ID);

        assertThat(response.monthlyEssentialExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedEmergencyFundTarget()).isEqualByComparingTo("0.00");
        assertThat(response.remainingAmountNeeded()).isEqualByComparingTo("0.00");
        assertThat(response.suggestedMonthlyContribution()).isEqualByComparingTo("0.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculate_negativeAndInvalidValuesHandledSafely() {
        // 6. negative/invalid values handled safely
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("10000.00")));

        // Negative disposable income
        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("-5000.00")));

        // Negative custom fund and invalid custom months
        EmergencyFundPlanResponse response = emergencyFundService.calculate(
                USER_ID, null, null, new BigDecimal("-20000.00"), -3);

        assertThat(response.currentEmergencyFund()).isEqualByComparingTo("0.00");
        assertThat(response.recommendedMonths()).isEqualTo(6);
        assertThat(response.recommendedEmergencyFundTarget()).isEqualByComparingTo("60000.00");
        assertThat(response.remainingAmountNeeded()).isEqualByComparingTo("60000.00");
        // Negative disposable income -> contribution is 0
        assertThat(response.suggestedMonthlyContribution()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_userIsolation_callsDependenciesWithUserIdOnly() {
        // 7. user isolation
        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(List.of(), BigDecimal.ZERO));

        emergencyFundService.calculate(USER_ID);

        verify(financialSummaryService).summarize(eq(USER_ID), any(), any());
        verify(financialGoalRepository).findByUserIdOrderByTargetDateAsc(eq(USER_ID));
        verify(investmentProfileRepository).findByUserId(eq(USER_ID));
    }

    @Test
    void calculate_dateFiltering_passesDateRangeToFinancialSummary() {
        // 8. date filtering
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(financialSummaryService.summarize(USER_ID, from, to))
                .thenReturn(createSummaryWithExpenses(List.of(), BigDecimal.ZERO));

        emergencyFundService.calculate(USER_ID, from, to);

        verify(financialSummaryService).summarize(USER_ID, from, to);
    }

    @Test
    void calculate_throwsWhenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> emergencyFundService.calculate(USER_ID, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be after to");
    }

    @Test
    void calculate_usesRiskPreferenceFromInvestmentProfileWhenAvailable() {
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("10000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("10000.00")));

        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), USER_ID);
        InvestmentProfile profile = new InvestmentProfile(user, InvestmentRiskPreference.AGGRESSIVE, 5);
        when(investmentProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        EmergencyFundPlanResponse response = emergencyFundService.calculate(USER_ID);

        // Aggressive risk -> 3 months
        assertThat(response.recommendedMonths()).isEqualTo(3);
        assertThat(response.recommendedEmergencyFundTarget()).isEqualByComparingTo("30000.00");
    }

    @Test
    void calculate_resolvesCurrentFundFromEmergencyFundGoalWhenUnspecified() {
        List<AmountByCategory> categories = List.of(
                new AmountByCategory(1L, "Rent", new BigDecimal("10000.00")));

        when(financialSummaryService.summarize(eq(USER_ID), any(), any()))
                .thenReturn(createSummaryWithExpenses(categories, new BigDecimal("10000.00")));

        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), USER_ID);
        FinancialGoal goal = new FinancialGoal(
                user, "Rainy Day", FinancialGoalType.EMERGENCY_FUND,
                new BigDecimal("60000.00"), new BigDecimal("15000.00"),
                LocalDate.of(2026, 12, 31), "Emergency savings");

        when(financialGoalRepository.findByUserIdOrderByTargetDateAsc(USER_ID)).thenReturn(List.of(goal));

        EmergencyFundPlanResponse response = emergencyFundService.calculate(USER_ID);

        assertThat(response.currentEmergencyFund()).isEqualByComparingTo("15000.00");
        assertThat(response.remainingAmountNeeded()).isEqualByComparingTo("45000.00");
    }
}
