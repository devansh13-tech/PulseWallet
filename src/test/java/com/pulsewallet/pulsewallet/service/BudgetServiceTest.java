package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import com.pulsewallet.pulsewallet.dto.BudgetRequest;
import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.entity.Budget;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.BudgetRepository;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private BudgetService budgetService;
    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, categoryRepository, userRepository);
        owner = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), OWNER_ID);
        otherUser = TestEntities.withId(new User("Bob", "bob@example.com", "hash"), OTHER_USER_ID);
    }

    @Test
    void create_rejectsAnEndDateBeforeTheStartDate() {
        BudgetRequest request = new BudgetRequest(
                new BigDecimal("500.00"), null, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> budgetService.create(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must not be before startDate");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void create_savesAValidBudget() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 20L);
        });
        BudgetRequest request = new BudgetRequest(
                new BigDecimal("500.00"), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        BudgetResponse response = budgetService.create(OWNER_ID, request);

        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void create_allowsSingleDayBudgetWhereStartEqualsEndDate() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 21L);
        });
        LocalDate singleDay = LocalDate.of(2026, 8, 15);
        BudgetRequest request = new BudgetRequest(
                new BigDecimal("250.00"), null, singleDay, singleDay);

        BudgetResponse response = budgetService.create(OWNER_ID, request);

        assertThat(response.startDate()).isEqualTo(singleDay);
        assertThat(response.endDate()).isEqualTo(singleDay);
    }

    @Test
    void create_resolvesOwnedCategorySuccessfully() {
        Category userCat = TestEntities.withId(new Category("Groceries", com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE, owner), 5L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(userCat));
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 22L);
        });

        BudgetRequest request = new BudgetRequest(
                new BigDecimal("1200.00"), 5L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        BudgetResponse response = budgetService.create(OWNER_ID, request);

        assertThat(response.categoryId()).isEqualTo(5L);
        assertThat(response.categoryName()).isEqualTo("Groceries");
    }

    @Test
    void create_rejectsCategoryOwnedByAnotherUser() {
        Category othersCat = TestEntities.withId(new Category("Secret", com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE, otherUser), 9L);
        when(categoryRepository.findById(9L)).thenReturn(Optional.of(othersCat));

        BudgetRequest request = new BudgetRequest(
                new BigDecimal("500.00"), 9L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThatThrownBy(() -> budgetService.create(OWNER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void create_acceptsSystemDefaultCategory() {
        Category systemCat = TestEntities.withId(new Category("Dining", com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE, null), 3L);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(systemCat));
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 23L);
        });

        BudgetRequest request = new BudgetRequest(
                new BigDecimal("800.00"), 3L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        BudgetResponse response = budgetService.create(OWNER_ID, request);

        assertThat(response.categoryId()).isEqualTo(3L);
        assertThat(response.categoryName()).isEqualTo("Dining");
    }

    @Test
    void get_rejectsABudgetOwnedByAnotherUser() {
        when(budgetRepository.findByIdAndUserId(20L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.get(20L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_returnsOnlyBudgetsForGivenUser() {
        Budget b1 = TestEntities.withId(new Budget(owner, null, new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), 101L);
        Budget b2 = TestEntities.withId(new Budget(owner, null, new BigDecimal("2000.00"), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), 102L);

        when(budgetRepository.findByUserId(OWNER_ID)).thenReturn(List.of(b1, b2));

        List<BudgetResponse> list = budgetService.list(OWNER_ID);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(101L);
        assertThat(list.get(1).id()).isEqualTo(102L);
    }

    @Test
    void update_modifiesOwnedBudgetSuccessfully() {
        Budget existing = TestEntities.withId(new Budget(owner, null, new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), 101L);
        when(budgetRepository.findByIdAndUserId(101L, OWNER_ID)).thenReturn(Optional.of(existing));

        BudgetRequest updateReq = new BudgetRequest(
                new BigDecimal("1500.00"), null, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 5));

        BudgetResponse updated = budgetService.update(101L, OWNER_ID, updateReq);

        assertThat(updated.amount()).isEqualByComparingTo("1500.00");
        assertThat(updated.startDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(updated.endDate()).isEqualTo(LocalDate.of(2026, 2, 5));
    }

    @Test
    void update_rejectsEndDateBeforeStartDate() {
        BudgetRequest updateReq = new BudgetRequest(
                new BigDecimal("1500.00"), null, LocalDate.of(2026, 2, 5), LocalDate.of(2026, 1, 5));

        assertThatThrownBy(() -> budgetService.update(101L, OWNER_ID, updateReq))
                .isInstanceOf(IllegalArgumentException.class);

        verify(budgetRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void update_rejectsBudgetOwnedByAnotherUser() {
        when(budgetRepository.findByIdAndUserId(101L, OTHER_USER_ID)).thenReturn(Optional.empty());

        BudgetRequest updateReq = new BudgetRequest(
                new BigDecimal("1500.00"), null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThatThrownBy(() -> budgetService.update(101L, OTHER_USER_ID, updateReq))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesOwnedBudget() {
        Budget existing = TestEntities.withId(new Budget(owner, null, new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), 101L);
        when(budgetRepository.findByIdAndUserId(101L, OWNER_ID)).thenReturn(Optional.of(existing));

        budgetService.delete(101L, OWNER_ID);

        verify(budgetRepository).delete(existing);
    }

    @Test
    void delete_rejectsBudgetOwnedByAnotherUser() {
        when(budgetRepository.findByIdAndUserId(101L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.delete(101L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(budgetRepository, never()).delete(any());
    }
}
