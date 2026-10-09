package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class FinancialSummaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    private FinancialSummaryService financialSummaryService;

    @BeforeEach
    void setUp() {
        financialSummaryService = new FinancialSummaryService(transactionRepository);
    }

    @Test
    void summarize_income50000Expenses32000_calculatesDisposable18000AndSavingsRate36Percent() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("32000.00"));

        FinancialSummaryResponse response = financialSummaryService.summarize(USER_ID, null, null);

        assertThat(response.totalIncome()).isEqualByComparingTo("50000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("32000.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("18000.00");
        assertThat(response.savingsRate()).isEqualByComparingTo("36.00");
    }

    @Test
    void summarize_income50000Expenses0_calculatesSavingsRate100Percent() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        FinancialSummaryResponse response = financialSummaryService.summarize(USER_ID, null, null);

        assertThat(response.totalIncome()).isEqualByComparingTo("50000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0");
        assertThat(response.disposableIncome()).isEqualByComparingTo("50000.00");
        assertThat(response.savingsRate()).isEqualByComparingTo("100.00");
    }

    @Test
    void summarize_income0Expenses0_calculatesSavingsRate0Percent() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        FinancialSummaryResponse response = financialSummaryService.summarize(USER_ID, null, null);

        assertThat(response.totalIncome()).isEqualByComparingTo("0");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0");
        assertThat(response.disposableIncome()).isEqualByComparingTo("0");
        assertThat(response.savingsRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void summarize_income20000Expenses25000_calculatesNegativeDisposableAndNegativeSavingsRate() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("20000.00"));
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("25000.00"));

        FinancialSummaryResponse response = financialSummaryService.summarize(USER_ID, null, null);

        assertThat(response.totalIncome()).isEqualByComparingTo("20000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("25000.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("-5000.00");
        assertThat(response.savingsRate()).isEqualByComparingTo("-25.00");
    }

    @Test
    void summarize_preservesUserIsolation() {
        financialSummaryService.summarize(USER_ID, null, null);

        verify(transactionRepository).sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class));
        verify(transactionRepository).sumAmountByUserIdAndTypeAndDateBetween(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class));
        verify(transactionRepository).sumByPeriod(eq(USER_ID), eq(TransactionType.INCOME), any(), any());
        verify(transactionRepository).sumByPeriod(eq(USER_ID), eq(TransactionType.EXPENSE), any(), any());
        verify(transactionRepository).sumExpensesByCategory(eq(USER_ID), any(), any());
        verify(transactionRepository).sumByMonth(eq(USER_ID), any(), any());
        verify(transactionRepository).sumExpensesByCategoryAndMonth(eq(USER_ID), any(), any());
        verify(transactionRepository).sumByMonthAndType(eq(USER_ID), any(), any());
    }

    @Test
    void summarize_preservesMonthlyAndDateFiltering() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        financialSummaryService.summarize(USER_ID, from, to);

        verify(transactionRepository).sumAmountByUserIdAndTypeAndDateBetween(USER_ID, TransactionType.INCOME, from, to);
        verify(transactionRepository).sumAmountByUserIdAndTypeAndDateBetween(USER_ID, TransactionType.EXPENSE, from, to);
        verify(transactionRepository).sumByPeriod(USER_ID, TransactionType.INCOME, from, to);
        verify(transactionRepository).sumByPeriod(USER_ID, TransactionType.EXPENSE, from, to);
        verify(transactionRepository).sumExpensesByCategory(USER_ID, from, to);
        verify(transactionRepository).sumByMonth(USER_ID, from, to);
        verify(transactionRepository).sumExpensesByCategoryAndMonth(USER_ID, from, to);
        verify(transactionRepository).sumByMonthAndType(USER_ID, from, to);
    }

    @Test
    void summarize_throwsWhenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> financialSummaryService.summarize(USER_ID, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be after to");
    }

    @Test
    void calculateSavingsRate_handlesNullsSafely() {
        assertThat(financialSummaryService.calculateSavingsRate(null, new BigDecimal("1000")))
                .isEqualByComparingTo("0.00");
        assertThat(financialSummaryService.calculateSavingsRate(new BigDecimal("1000"), null))
                .isEqualByComparingTo("0.00");
        assertThat(financialSummaryService.calculateSavingsRate(BigDecimal.ZERO, new BigDecimal("1000")))
                .isEqualByComparingTo("0.00");
    }
}
