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
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.FinancialGoalType;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
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

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC);
    private FinancialGoalService service;

    @BeforeEach
    void setUp() {
        service = new FinancialGoalService(goalRepository, userRepository, summaryService, clock);
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

    private FinancialSummaryResponse summary(int income, int expenses) {
        return new FinancialSummaryResponse(null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.INCOME,
                                BigDecimal.valueOf(income)),
                        new FinancialSummaryResponse.MonthlyTypeAmount(2026, 8, TransactionType.EXPENSE,
                                BigDecimal.valueOf(expenses))));
    }
}