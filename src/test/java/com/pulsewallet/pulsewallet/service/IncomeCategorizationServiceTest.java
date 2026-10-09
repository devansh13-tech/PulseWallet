package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class IncomeCategorizationServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;

    private IncomeCategorizationService categorizationService;

    private Category salary;
    private Category freelance;
    private Category investmentReturns;
    private Category otherIncome;
    private Category groceries; // Expense category to test safety filter

    @BeforeEach
    void setUp() {
        categorizationService = new IncomeCategorizationService(categoryRepository);

        salary = TestEntities.withId(new Category("Salary", TransactionType.INCOME, null), 1L);
        freelance = TestEntities.withId(new Category("Freelance", TransactionType.INCOME, null), 2L);
        investmentReturns = TestEntities.withId(new Category("Investment Returns", TransactionType.INCOME, null), 3L);
        otherIncome = TestEntities.withId(new Category("Other Income", TransactionType.INCOME, null), 4L);
        groceries = TestEntities.withId(new Category("Groceries", TransactionType.EXPENSE, null), 5L);
    }

    private void stubCategories() {
        when(categoryRepository.findByUserIdOrUserIsNull(USER_ID))
                .thenReturn(List.of(salary, freelance, investmentReturns, otherIncome, groceries));
    }

    @Test
    void categorize_salaryCreditedMapsToSalary() {
        stubCategories();

        Category result = categorizationService.categorize("Salary credited", USER_ID);

        assertThat(result).isSameAs(salary);
    }

    @Test
    void categorize_monthlySalaryMapsToSalary() {
        stubCategories();

        Category result = categorizationService.categorize("Monthly salary", USER_ID);

        assertThat(result).isSameAs(salary);
    }

    @Test
    void categorize_payrollCreditMapsToSalary() {
        stubCategories();

        Category result = categorizationService.categorize("Payroll credit", USER_ID);

        assertThat(result).isSameAs(salary);
    }

    @Test
    void categorize_freelanceProjectPaymentMapsToFreelance() {
        stubCategories();

        Category result = categorizationService.categorize("Freelance project payment", USER_ID);

        assertThat(result).isSameAs(freelance);
    }

    @Test
    void categorize_clientPaymentMapsToFreelance() {
        stubCategories();

        Category result = categorizationService.categorize("Client payment", USER_ID);

        assertThat(result).isSameAs(freelance);
    }

    @Test
    void categorize_dividendReceivedMapsToInvestmentReturns() {
        stubCategories();

        Category result = categorizationService.categorize("Dividend received", USER_ID);

        assertThat(result).isSameAs(investmentReturns);
    }

    @Test
    void categorize_interestCreditedMapsToInvestmentReturns() {
        stubCategories();

        Category result = categorizationService.categorize("Interest credited", USER_ID);

        assertThat(result).isSameAs(investmentReturns);
    }

    @Test
    void categorize_unknownIncomeFallsBackToOtherIncome() {
        stubCategories();
        when(categoryRepository.findByNameAndUserIsNull("Other Income"))
                .thenReturn(Optional.of(otherIncome));

        Category result = categorizationService.categorize("Money received", USER_ID);

        assertThat(result).isSameAs(otherIncome);
    }

    @Test
    void categorize_nullDescriptionHandledSafelyFallsBackToOtherIncome() {
        when(categoryRepository.findByNameAndUserIsNull("Other Income"))
                .thenReturn(Optional.of(otherIncome));

        Category result = categorizationService.categorize(null, USER_ID);

        assertThat(result).isSameAs(otherIncome);
    }

    @Test
    void categorize_emptyDescriptionFallsBackToOtherIncome() {
        when(categoryRepository.findByNameAndUserIsNull("Other Income"))
                .thenReturn(Optional.of(otherIncome));

        Category result = categorizationService.categorize("   ", USER_ID);

        assertThat(result).isSameAs(otherIncome);
    }

    @Test
    void categorize_neverReturnsAnExpenseCategoryEvenIfDescriptionContainsItsName() {
        stubCategories();
        when(categoryRepository.findByNameAndUserIsNull("Other Income"))
                .thenReturn(Optional.of(otherIncome));

        Category result = categorizationService.categorize("Groceries refund credit", USER_ID);

        assertThat(result).isSameAs(otherIncome);
        assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void categorize_matchesCustomUserOwnedIncomeCategory() {
        User user = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), USER_ID);
        Category customIncome = TestEntities.withId(new Category("Side Business", TransactionType.INCOME, user), 50L);
        when(categoryRepository.findByUserIdOrUserIsNull(USER_ID))
                .thenReturn(List.of(salary, freelance, investmentReturns, otherIncome, customIncome));

        Category result = categorizationService.categorize("Payout from side business", USER_ID);

        assertThat(result).isSameAs(customIncome);
    }
}
