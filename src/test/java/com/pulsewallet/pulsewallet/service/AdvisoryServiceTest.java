package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.MonthlyTypeTotalProjection;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class AdvisoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private AdvisoryService advisoryService;

    @BeforeEach
    void setUp() {
        advisoryService = new AdvisoryService(transactionRepository, Clock.fixed(
                Instant.parse("2026-08-15T12:00:00Z"), ZoneId.of("UTC")));
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
                        monthTotal(2026, 7, TransactionType.INCOME, new BigDecimal("50000.00")),
                        monthTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("50000.00"))));

        AdvisoryResponse response = advisoryService.advice(2L);

        assertThat(response.disposableIncome()).isZero();
        assertThat(response.recommendedSavings()).isZero();
        assertThat(response.recommendedInvestment()).isZero();
    }

    @Test
    void advisory_handlesNegativeDisposableIncome() {
        when(transactionRepository.sumByMonthAndType(eq(3L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 7, TransactionType.INCOME, new BigDecimal("40000.00")),
                        monthTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("70000.00"))));

        AdvisoryResponse response = advisoryService.advice(3L);

        assertThat(response.disposableIncome()).isEqualByComparingTo("-30000.00");
        assertThat(response.recommendedSavings()).isZero();
        assertThat(response.recommendedInvestment()).isZero();
    }

    @Test
    void advisory_handlesHighExpensesAndLowIncome() {
        when(transactionRepository.sumByMonthAndType(eq(4L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 7, TransactionType.INCOME, new BigDecimal("10000.00")),
                        monthTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("25000.00"))));

        AdvisoryResponse response = advisoryService.advice(4L);

        assertThat(response.monthlyIncome()).isEqualByComparingTo("10000.00");
        assertThat(response.monthlyExpenses()).isEqualByComparingTo("25000.00");
        assertThat(response.recommendedInvestment()).isZero();
        assertThat(response.emergencyFundTarget()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void advisory_handlesPositiveSavingsCapacity() {
        when(transactionRepository.sumByMonthAndType(eq(6L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 7, TransactionType.INCOME, new BigDecimal("150000.00")),
                        monthTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("80000.00")),
                        monthTotal(2026, 8, TransactionType.INCOME, new BigDecimal("150000.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("85000.00"))));

        AdvisoryResponse response = advisoryService.advice(6L);

        assertThat(response.recommendedSavings()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.recommendedInvestment()).isGreaterThan(BigDecimal.ZERO);
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
