package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.BudgetRequest;
import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.entity.Budget;
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

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, categoryRepository, userRepository);
        owner = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), OWNER_ID);
    }

    @Test
    void create_rejectsAnEndDateBeforeTheStartDate() {
        BudgetRequest request = new BudgetRequest(
                new BigDecimal("500.00"), null, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> budgetService.create(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class);

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
    }

    @Test
    void get_rejectsABudgetOwnedByAnotherUser() {
        when(budgetRepository.findByIdAndUserId(20L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.get(20L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
