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

import com.pulsewallet.pulsewallet.dto.ForecastResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.MonthlyTypeTotalProjection;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private ForecastService forecastService;

    @BeforeEach
    void setUp() {
        forecastService = new ForecastService(transactionRepository, Clock.fixed(
                Instant.parse("2026-08-15T12:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void forecast_usesAverageOfMultipleMonthsOfExpenseHistory() {
        when(transactionRepository.sumByMonthAndType(eq(5L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2025, 8, TransactionType.EXPENSE, new BigDecimal("2000.00")),
                        monthTotal(2025, 9, TransactionType.EXPENSE, new BigDecimal("2500.00")),
                        monthTotal(2025, 10, TransactionType.EXPENSE, new BigDecimal("3000.00"))));

        ForecastResponse response = forecastService.forecast(5L);

        assertThat(response.monthsIncluded()).isEqualTo(3);
        assertThat(response.averageMonthlyExpense()).isEqualByComparingTo("2500.00");
        assertThat(response.forecastMonthlyExpense()).isEqualByComparingTo("2500.00");
        assertThat(response.monthlyExpenses()).hasSize(3);
    }

    @Test
    void forecast_handlesSingleMonthOfHistory() {
        when(transactionRepository.sumByMonthAndType(eq(5L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 1, TransactionType.EXPENSE, new BigDecimal("4200.00"))));

        ForecastResponse response = forecastService.forecast(5L);

        assertThat(response.monthsIncluded()).isEqualTo(1);
        assertThat(response.averageMonthlyExpense()).isEqualByComparingTo("4200.00");
        assertThat(response.forecastMonthlyExpense()).isEqualByComparingTo("4200.00");
    }

    @Test
    void forecast_returnsZeroValuesWhenNoTransactionsExist() {
        when(transactionRepository.sumByMonthAndType(eq(5L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of());

        ForecastResponse response = forecastService.forecast(5L);

        assertThat(response.monthsIncluded()).isZero();
        assertThat(response.averageMonthlyExpense()).isZero();
        assertThat(response.forecastMonthlyExpense()).isZero();
        assertThat(response.monthlyExpenses()).isEmpty();
    }

    @Test
    void forecast_handlesInsufficientHistoryGracefully() {
        when(transactionRepository.sumByMonthAndType(eq(5L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("3500.00"))));

        ForecastResponse response = forecastService.forecast(5L);

        assertThat(response.monthsIncluded()).isEqualTo(1);
        assertThat(response.forecastBasis()).contains("average");
    }

    @Test
    void forecast_onlyUsesTheAuthenticatedUsersData() {
        when(transactionRepository.sumByMonthAndType(eq(9L), eq(LocalDate.of(2025, 8, 1)),
                eq(LocalDate.of(2026, 8, 15))))
                .thenReturn(List.of(
                        monthTotal(2026, 7, TransactionType.EXPENSE, new BigDecimal("6600.00")),
                        monthTotal(2026, 8, TransactionType.EXPENSE, new BigDecimal("7200.00"))));

        ForecastResponse response = forecastService.forecast(9L);

        assertThat(response.averageMonthlyExpense()).isEqualByComparingTo("6900.00");
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
