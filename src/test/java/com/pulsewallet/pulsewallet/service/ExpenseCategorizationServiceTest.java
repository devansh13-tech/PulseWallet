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
    private Category transportation;
    private Category otherExpense;

    @BeforeEach
    void setUp() {
        categorizationService = new ExpenseCategorizationService(categoryRepository);

        groceries = TestEntities.withId(new Category("Groceries", TransactionType.EXPENSE, null), 1L);
        transportation = TestEntities.withId(new Category("Transportation", TransactionType.EXPENSE, null), 2L);
        otherExpense = TestEntities.withId(new Category("Other Expense", TransactionType.EXPENSE, null), 3L);
    }

    private void stubCategories() {
        when(categoryRepository.findByUserIdOrUserIsNull(USER_ID))
                .thenReturn(List.of(groceries, transportation, otherExpense));
    }

    @Test
    void categorize_matchesGroceryKeywords() {
        stubCategories();

        Category result = categorizationService.categorize("Bought milk from the store", USER_ID);

        assertThat(result).isSameAs(groceries);
    }

    @Test
    void categorize_matchesTransportationKeywords() {
        stubCategories();

        Category result = categorizationService.categorize("Uber ride to office", USER_ID);

        assertThat(result).isSameAs(transportation);
    }

    @Test
    void categorize_fallsBackToOtherExpenseWhenNoRuleMatches() {
        stubCategories();
        when(categoryRepository.findByNameAndUserIsNull("Other Expense"))
                .thenReturn(Optional.of(otherExpense));

        Category result = categorizationService.categorize("Random purchase xyz", USER_ID);

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
}
