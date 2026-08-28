package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.BudgetPlanRequest;
import com.pulsewallet.pulsewallet.dto.BudgetPlanResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class BudgetPlanServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    @Mock
    private TransactionRepository transactionRepository;

    private BudgetPlanService budgetPlanService;

    @BeforeEach
    void setUp() {
        budgetPlanService = new BudgetPlanService(transactionRepository);
    }

    @Test
    void createPlan_calculatesPositiveDisposableIncomeAndAllocation() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(7L, TransactionType.EXPENSE, START, END))
                .thenReturn(new BigDecimal("5000.00"));

        BudgetPlanResponse response = budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("10000.00"), START, END));

        assertThat(response.salary()).isEqualByComparingTo("10000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("5000.00");
        assertThat(response.disposableIncome()).isEqualByComparingTo("5000.00");
        assertThat(response.recommendedSavings()).isEqualByComparingTo("1000.00");
        assertThat(response.recommendedInvestment()).isEqualByComparingTo("500.00");
        assertThat(response.recommendedSpending()).isEqualByComparingTo("3500.00");
    }

    @Test
    void createPlan_handlesZeroExpenses() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(7L, TransactionType.EXPENSE, START, END))
                .thenReturn(BigDecimal.ZERO);

        BudgetPlanResponse response = budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("12000.00"), START, END));

        assertThat(response.totalExpenses()).isZero();
        assertThat(response.disposableIncome()).isEqualByComparingTo("12000.00");
        assertThat(response.recommendedSavings()).isEqualByComparingTo("2400.00");
        assertThat(response.recommendedInvestment()).isEqualByComparingTo("1200.00");
        assertThat(response.recommendedSpending()).isEqualByComparingTo("8400.00");
    }

    @Test
    void createPlan_handlesExpensesEqualToIncome() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(7L, TransactionType.EXPENSE, START, END))
                .thenReturn(new BigDecimal("8000.00"));

        BudgetPlanResponse response = budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("8000.00"), START, END));

        assertThat(response.disposableIncome()).isZero();
        assertThat(response.recommendedSavings()).isZero();
        assertThat(response.recommendedInvestment()).isZero();
        assertThat(response.recommendedSpending()).isZero();
    }

    @Test
    void createPlan_handlesNegativeDisposableIncomeWithoutNegativeRecommendations() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(7L, TransactionType.EXPENSE, START, END))
                .thenReturn(new BigDecimal("15000.00"));

        BudgetPlanResponse response = budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("12000.00"), START, END));

        assertThat(response.disposableIncome()).isEqualByComparingTo("-3000.00");
        assertThat(response.recommendedSavings()).isZero();
        assertThat(response.recommendedInvestment()).isZero();
        assertThat(response.recommendedSpending()).isZero();
    }

    @Test
    void createPlan_handlesNoTransactions() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(7L, TransactionType.EXPENSE, START, END))
                .thenReturn(null);

        BudgetPlanResponse response = budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("9000.00"), START, END));

        assertThat(response.totalExpenses()).isZero();
        assertThat(response.disposableIncome()).isEqualByComparingTo("9000.00");
        assertThat(response.recommendedSavings()).isEqualByComparingTo("1800.00");
    }

    @Test
    void createPlan_rejectsInvalidSalary() {
        assertThatThrownBy(() -> budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("0.00"), START, END)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salary");
    }

    @Test
    void createPlan_rejectsInvalidDateRange() {
        assertThatThrownBy(() -> budgetPlanService.createPlan(7L, new BudgetPlanRequest(
                new BigDecimal("5000.00"), END, START)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate");
    }

    @Test
    void createPlan_isolatesTransactionsToTheAuthenticatedUser() {
        when(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(22L, TransactionType.EXPENSE, START, END))
                .thenReturn(new BigDecimal("2500.00"));

        BudgetPlanResponse response = budgetPlanService.createPlan(22L, new BudgetPlanRequest(
                new BigDecimal("6000.00"), START, END));

        assertThat(response.disposableIncome()).isEqualByComparingTo("3500.00");
    }

    @Test
    void createPlan_rejectsNullUserId() {
        assertThatThrownBy(() -> budgetPlanService.createPlan(
                null,
                new BudgetPlanRequest(
                        new BigDecimal("5000.00"),
                        START,
                        END)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }
}
