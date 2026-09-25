package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.entity.InvestmentRiskPreference;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;

@ExtendWith(MockitoExtension.class)
class FinancialPlanServiceTest {

    @Mock
    private FinancialSummaryService summaryService;
    @Mock
    private FinancialGoalService goalService;
    @Mock
    private InvestmentProfileRepository profileRepository;

    private FinancialPlanService service;

    @BeforeEach
    void setUp() {
        service = new FinancialPlanService(summaryService, goalService, profileRepository);
    }

    @Test
    void prioritizesActiveGoalsBufferAndInvestmentWithoutExceedingSurplus() {
        Long userId = 1L;
        when(summaryService.summarize(userId, null, null)).thenReturn(summary(80000, 35000));
        when(goalService.list(userId)).thenReturn(List.of(goal("Bike", "16000", "ON_TRACK"),
                goal("Done", "9000", "COMPLETED")));
        InvestmentProfile profile = new InvestmentProfile(null, InvestmentRiskPreference.MODERATE, 8);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        var result = service.calculate(userId);

        assertThat(result.monthlyLeft()).isEqualByComparingTo("45000");
        assertThat(result.goalSavingsRequired()).isEqualByComparingTo("16000");
        assertThat(result.emergencyBufferTarget()).isEqualByComparingTo("105000");
        assertThat(result.recommendedEmergencyAllocation()).isEqualByComparingTo("8750");
        assertThat(result.recommendedInvestmentAmount()).isEqualByComparingTo("20250");
        assertThat(result.flexibleSpendingAmount()).isZero();
        assertThat(result.allocation().stream().map(a -> a.percentage())
            .reduce(BigDecimal.ZERO, (total, percentage) -> total.add(percentage)))
                .isEqualByComparingTo("100");
        assertThat(result.recommendedInvestmentAmount()).isLessThanOrEqualTo(result.monthlyLeft());
    }

    @Test
    void doesNotRecommendNegativeSavingsOrInvestmentWhenExpensesExceedIncome() {
        Long userId = 2L;
        when(summaryService.summarize(userId, null, null)).thenReturn(summary(1000, 1400));
        when(goalService.list(userId)).thenReturn(List.of(goal("House", "2000", "NEEDS_ATTENTION")));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var result = service.calculate(userId);

        assertThat(result.monthlyLeft()).isEqualByComparingTo("-400");
        assertThat(result.recommendedEmergencyAllocation()).isZero();
        assertThat(result.recommendedInvestmentAmount()).isZero();
        assertThat(result.flexibleSpendingAmount()).isZero();
        assertThat(result.warnings()).anyMatch(message -> message.contains("exceed"));
        assertThat(result.warnings()).anyMatch(message -> message.contains("not configured"));
    }

    @Test
    void rejectsNullUserId() {
        assertThatThrownBy(() -> service.calculate(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private FinancialGoalResponse goal(String name, String monthlySaving, String status) {
        return new FinancialGoalResponse(1L, name, "CUSTOM", money("100000"), money("1000"), money("99000"),
                money("1"), 10, money(monthlySaving), status, null, null, null);
    }

    private FinancialSummaryResponse summary(int income, int expenses) {
        return new FinancialSummaryResponse(null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME,
                                money(String.valueOf(income))),
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE,
                                money(String.valueOf(expenses)))));
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
