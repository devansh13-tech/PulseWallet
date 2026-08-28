package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.pulsewallet.pulsewallet.dto.TransactionRequest;
import com.pulsewallet.pulsewallet.dto.TransactionResponse;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseCategorizationService expenseCategorizationService;

    private TransactionService transactionService;
    private User owner;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, categoryRepository, userRepository, expenseCategorizationService);
        owner = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), OWNER_ID);
    }

    @Test
    void create_savesAnUncategorizedTransactionWhenNoCategoryIdIsGiven() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(expenseCategorizationService.categorize("Groceries", OWNER_ID)).thenReturn(null);
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("49.99"), "Groceries", null, TransactionType.EXPENSE, LocalDate.of(2026, 8, 1));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 10L);
        });

        TransactionResponse response = transactionService.create(OWNER_ID, request);

        assertThat(response.categoryId()).isNull();
        assertThat(response.amount()).isEqualByComparingTo("49.99");
        verify(expenseCategorizationService).categorize("Groceries", OWNER_ID);
    }

    @Test
    void create_rejectsACategoryIdThatBelongsToAnotherUser() {
        User otherOwner = TestEntities.withId(new User("Bob", "bob@example.com", "hash"), OTHER_USER_ID);
        Category othersCategory = TestEntities.withId(
                new Category("Bob's Category", TransactionType.EXPENSE, otherOwner), 99L);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(categoryRepository.findById(99L)).thenReturn(Optional.of(othersCategory));
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("10.00"), null, 99L, TransactionType.EXPENSE, LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> transactionService.create(OWNER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void get_returnsTheTransactionWhenTheOwnerAsks() {
        Transaction transaction = TestEntities.withId(
                new Transaction(owner, new BigDecimal("5.00"), null, null, TransactionType.EXPENSE, LocalDate.now()),
                5L);
        when(transactionRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.get(5L, OWNER_ID);

        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void get_returns404StyleExceptionWhenAnotherUserAsksForIt() {
        // findByIdAndUserId with the wrong user id simply finds nothing - this
        // is the actual mechanism that prevents cross-user reads, exercised
        // the same way the repository would behave against a real database.
        when(transactionRepository.findByIdAndUserId(5L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.get(5L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_doesNotDeleteATransactionOwnedBySomeoneElse() {
        when(transactionRepository.findByIdAndUserId(5L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(5L, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void create_autoCategorizesAnExpenseWhenNoCategoryIsProvided() {
        Category groceries = TestEntities.withId(
                new Category("Groceries", TransactionType.EXPENSE, null), 5L);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(expenseCategorizationService.categorize("Bought milk from supermarket", OWNER_ID))
                .thenReturn(groceries);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 11L);
        });
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("200.00"), "Bought milk from supermarket", null,
                TransactionType.EXPENSE, LocalDate.of(2026, 8, 5));

        TransactionResponse response = transactionService.create(OWNER_ID, request);

        assertThat(response.categoryId()).isEqualTo(5L);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        verify(expenseCategorizationService).categorize("Bought milk from supermarket", OWNER_ID);
    }

    @Test
    void create_doesNotAutoCategorizeWhenAnExplicitCategoryIsProvided() {
        Category shopping = TestEntities.withId(
                new Category("Shopping", TransactionType.EXPENSE, null), 7L);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(shopping));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 12L);
        });
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("150.00"), "Bought milk from supermarket", 7L,
                TransactionType.EXPENSE, LocalDate.of(2026, 8, 5));

        TransactionResponse response = transactionService.create(OWNER_ID, request);

        assertThat(response.categoryId()).isEqualTo(7L);
        verify(expenseCategorizationService, never()).categorize(any(), eq(OWNER_ID));
    }

    @Test
    void create_doesNotAutoCategorizeIncomeTransactions() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            return TestEntities.withId(saved, 13L);
        });
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50000.00"), "Monthly salary", null,
                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

        TransactionResponse response = transactionService.create(OWNER_ID, request);

        assertThat(response.categoryId()).isNull();
        verify(expenseCategorizationService, never()).categorize(any(), eq(OWNER_ID));
    }

    @Test
    void update_autoCategorizesAnExpenseWhenNoCategoryIsProvided() {
        Transaction existing = TestEntities.withId(
                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                        TransactionType.EXPENSE, LocalDate.of(2026, 8, 1)), 20L);
        Category transportation = TestEntities.withId(
                new Category("Transportation", TransactionType.EXPENSE, null), 9L);
        when(transactionRepository.findByIdAndUserId(20L, OWNER_ID)).thenReturn(Optional.of(existing));
        when(expenseCategorizationService.categorize("Uber to airport", OWNER_ID))
                .thenReturn(transportation);
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("300.00"), "Uber to airport", null,
                TransactionType.EXPENSE, LocalDate.of(2026, 8, 5));

        TransactionResponse response = transactionService.update(20L, OWNER_ID, request);

        assertThat(response.categoryId()).isEqualTo(9L);
        assertThat(response.categoryName()).isEqualTo("Transportation");
        verify(expenseCategorizationService).categorize("Uber to airport", OWNER_ID);
    }

    @Test
    void update_doesNotAutoCategorizeWhenAnExplicitCategoryIsProvided() {
        Transaction existing = TestEntities.withId(
                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                        TransactionType.EXPENSE, LocalDate.of(2026, 8, 1)), 21L);
        Category shopping = TestEntities.withId(
                new Category("Shopping", TransactionType.EXPENSE, null), 7L);
        when(transactionRepository.findByIdAndUserId(21L, OWNER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(shopping));
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("500.00"), "Uber to airport", 7L,
                TransactionType.EXPENSE, LocalDate.of(2026, 8, 5));

        TransactionResponse response = transactionService.update(21L, OWNER_ID, request);

        assertThat(response.categoryId()).isEqualTo(7L);
        verify(expenseCategorizationService, never()).categorize(any(), eq(OWNER_ID));
    }

    @Test
    void update_doesNotAutoCategorizeIncomeTransactions() {
        Transaction existing = TestEntities.withId(
                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                        TransactionType.INCOME, LocalDate.of(2026, 8, 1)), 22L);
        when(transactionRepository.findByIdAndUserId(22L, OWNER_ID)).thenReturn(Optional.of(existing));
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("60000.00"), "Updated salary", null,
                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

        TransactionResponse response = transactionService.update(22L, OWNER_ID, request);

        assertThat(response.categoryId()).isNull();
        verify(expenseCategorizationService, never()).categorize(any(), eq(OWNER_ID));
    }
}
