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
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class ExpenseCategorizationServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;

    private ExpenseCategorizationService categorizationService;

    private Category groceries;
    private Category foodAndDining;
    private Category rent;
    private Category utilities;
    private Category transportation;
    private Category healthcare;
    private Category entertainment;
    private Category shopping;
    private Category education;
    private Category otherExpense;
    private Category salary;

    @BeforeEach
    void setUp() {
        categorizationService = new ExpenseCategorizationService(categoryRepository);

        groceries = TestEntities.withId(new Category("Groceries", TransactionType.EXPENSE, null), 1L);
        foodAndDining = TestEntities.withId(new Category("Food & Dining", TransactionType.EXPENSE, null), 2L);
        rent = TestEntities.withId(new Category("Rent", TransactionType.EXPENSE, null), 3L);
        utilities = TestEntities.withId(new Category("Utilities", TransactionType.EXPENSE, null), 4L);
        transportation = TestEntities.withId(new Category("Transportation", TransactionType.EXPENSE, null), 5L);
        healthcare = TestEntities.withId(new Category("Healthcare", TransactionType.EXPENSE, null), 6L);
        entertainment = TestEntities.withId(new Category("Entertainment", TransactionType.EXPENSE, null), 7L);
        shopping = TestEntities.withId(new Category("Shopping", TransactionType.EXPENSE, null), 8L);
        education = TestEntities.withId(new Category("Education", TransactionType.EXPENSE, null), 9L);
        otherExpense = TestEntities.withId(new Category("Other Expense", TransactionType.EXPENSE, null), 10L);
        salary = TestEntities.withId(new Category("Salary", TransactionType.INCOME, null), 11L);
    }

    private void stubCategories() {
        when(categoryRepository.findByUserIdOrUserIsNull(USER_ID))
                .thenReturn(List.of(
                        salary,
                        groceries,
                        foodAndDining,
                        rent,
                        utilities,
                        transportation,
                        healthcare,
                        entertainment,
                        shopping,
                        education,
                        otherExpense));
    }

    @Test
    void categorize_swiggyMapsToFoodAndDining() {
        stubCategories();

        Category result = categorizationService.categorize("Swiggy dinner", USER_ID);

        assertThat(result).isSameAs(foodAndDining);
    }

    @Test
    void categorize_uberMapsToTransportation() {
        stubCategories();

        Category result = categorizationService.categorize("Uber ride", USER_ID);

        assertThat(result).isSameAs(transportation);
    }

    @Test
    void categorize_electricityBillMapsToUtilities() {
        stubCategories();

        Category result = categorizationService.categorize("Electricity bill", USER_ID);

        assertThat(result).isSameAs(utilities);
    }

    @Test
    void categorize_amazonPurchaseMapsToShopping() {
        stubCategories();

        Category result = categorizationService.categorize("Amazon purchase", USER_ID);

        assertThat(result).isSameAs(shopping);
    }

    @Test
    void categorize_pharmacyMapsToHealthcare() {
        stubCategories();

        Category result = categorizationService.categorize("Pharmacy", USER_ID);

        assertThat(result).isSameAs(healthcare);
    }

    @Test
    void categorize_collegeFeesMapsToEducation() {
        stubCategories();

        Category result = categorizationService.categorize("College fees", USER_ID);

        assertThat(result).isSameAs(education);
    }

    @Test
    void categorize_rentPaymentMapsToRent() {
        stubCategories();

        Category result = categorizationService.categorize("Rent payment", USER_ID);

        assertThat(result).isSameAs(rent);
    }

    @Test
    void categorize_netflixMapsToEntertainment() {
        stubCategories();

        Category result = categorizationService.categorize("Netflix", USER_ID);

        assertThat(result).isSameAs(entertainment);
    }

    @Test
    void categorize_milkMapsToGroceries() {
        stubCategories();

        Category result = categorizationService.categorize("Milk", USER_ID);

        assertThat(result).isSameAs(groceries);
    }

    @Test
    void categorize_unknownExpenseFallsBackToOtherExpense() {
        stubCategories();
        when(categoryRepository.findByNameAndUserIsNull("Other Expense"))
                .thenReturn(Optional.of(otherExpense));

        Category result = categorizationService.categorize("Unknown local purchase", USER_ID);

        assertThat(result).isSameAs(otherExpense);
    }

    @Test
    void categorize_returnsNullForNullDescription() {
        Category result = categorizationService.categorize(null, USER_ID);

        assertThat(result).isNull();
    }

    @Test
    void categorize_fallsBackToOtherExpenseForEmptyDescription() {
        when(categoryRepository.findByUserIdOrUserIsNull(USER_ID))
                .thenReturn(List.of(groceries, transportation));
        when(categoryRepository.findByNameAndUserIsNull("Other Expense"))
                .thenReturn(Optional.of(otherExpense));

        Category result = categorizationService.categorize("   ", USER_ID);

        assertThat(result).isSameAs(otherExpense);
    }

    @Test
    void categorize_ignoresIncomeCategories() {
        stubCategories();
        when(categoryRepository.findByNameAndUserIsNull("Other Expense"))
                .thenReturn(Optional.of(otherExpense));

        Category result = categorizationService.categorize("Salary bonus deduction", USER_ID);

        assertThat(result).isSameAs(otherExpense);
    }
}
