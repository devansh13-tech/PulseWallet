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

import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;
import com.pulsewallet.pulsewallet.dto.TransactionRequest;
import com.pulsewallet.pulsewallet.dto.TransactionResponse;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.FraudAlert;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.FraudAlertRepository;
import com.pulsewallet.pulsewallet.repository.NotificationRepository;
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

        @Mock
        private IncomeCategorizationService incomeCategorizationService;

        @Mock
        private FraudDetectionService fraudDetectionService;

        @Mock
        private FraudAlertRepository fraudAlertRepository;

        @Mock
        private NotificationRepository notificationRepository;

        private TransactionService transactionService;
        private User owner;

        @BeforeEach
        void setUp() {
                transactionService = new TransactionService(
                                transactionRepository,
                                categoryRepository,
                                userRepository,
                                expenseCategorizationService,
                                incomeCategorizationService,
                                fraudDetectionService,
                                fraudAlertRepository,
                                notificationRepository);
                owner = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), OWNER_ID);
        }

        @Test
        void create_savesAnUncategorizedTransactionWhenNoCategoryIdIsGiven() {
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(expenseCategorizationService.categorize("Groceries", OWNER_ID)).thenReturn(null);
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("49.99"), "Groceries", null, TransactionType.EXPENSE,
                                LocalDate.of(2026, 8, 1));
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
                                new Transaction(owner, new BigDecimal("5.00"), null, null, TransactionType.EXPENSE,
                                                LocalDate.now()),
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
        void create_autoCategorizesIncomeWhenNoCategoryIsProvided() {
                Category salary = TestEntities.withId(
                                new Category("Salary", TransactionType.INCOME, null), 1L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(incomeCategorizationService.categorize("Monthly salary", OWNER_ID)).thenReturn(salary);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 13L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("50000.00"), "Monthly salary", null,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(1L);
                assertThat(response.categoryName()).isEqualTo("Salary");
                verify(incomeCategorizationService).categorize("Monthly salary", OWNER_ID);
                verify(expenseCategorizationService, never()).categorize(any(), any());
                verify(fraudDetectionService, never()).checkFraud(any());
        }

        @Test
        void create_preservesExplicitCategoryForIncomeTransaction() {
                Category freelance = TestEntities.withId(
                                new Category("Freelance", TransactionType.INCOME, null), 2L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(categoryRepository.findById(2L)).thenReturn(Optional.of(freelance));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 30L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("25000.00"), "Salary credited", 2L,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(2L);
                assertThat(response.categoryName()).isEqualTo("Freelance");
                verify(incomeCategorizationService, never()).categorize(any(), any());
                verify(expenseCategorizationService, never()).categorize(any(), any());
        }

        @Test
        void create_preservesExplicitSalaryForIncomeTransaction() {
                Category salary = TestEntities.withId(
                                new Category("Salary", TransactionType.INCOME, null), 1L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(categoryRepository.findById(1L)).thenReturn(Optional.of(salary));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 31L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("1000.00"), "Dividend received", 1L,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(1L);
                assertThat(response.categoryName()).isEqualTo("Salary");
                verify(incomeCategorizationService, never()).categorize(any(), any());
        }

        @Test
        void create_handlesNullDescriptionForIncomeSafely() {
                Category otherIncome = TestEntities.withId(
                                new Category("Other Income", TransactionType.INCOME, null), 4L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(incomeCategorizationService.categorize(null, OWNER_ID)).thenReturn(otherIncome);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 32L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("500.00"), null, null,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.description()).isNull();
                assertThat(response.categoryId()).isEqualTo(4L);
                assertThat(response.categoryName()).isEqualTo("Other Income");
                verify(incomeCategorizationService).categorize(null, OWNER_ID);
        }

        @Test
        void create_expenseTransactionDoesNotTriggerIncomeCategorization() {
                Category shopping = TestEntities.withId(
                                new Category("Shopping", TransactionType.EXPENSE, null), 8L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(expenseCategorizationService.categorize("Salary shoes", OWNER_ID)).thenReturn(shopping);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 33L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("2999.00"), "Salary shoes", null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(8L);
                verify(expenseCategorizationService).categorize("Salary shoes", OWNER_ID);
                verify(incomeCategorizationService, never()).categorize(any(), any());
        }

        @Test
        void create_triggersFraudCheckForExpenseTransactions() {
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 14L);
                });
                when(fraudDetectionService.checkFraud(any()))
                                .thenReturn(new FraudCheckResponse(false, 0.02, 2.0, "LOW"));
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("149.62"), "Coffee shop purchase", null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 8));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.amount()).isEqualByComparingTo("149.62");
                verify(fraudDetectionService).checkFraud(any());
                verify(fraudAlertRepository, never()).save(any());
        }

        @Test
        void create_persistsAnAlertForAFraudulentExpense() {
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 15L);
                });
                when(fraudDetectionService.checkFraud(any()))
                                .thenReturn(new FraudCheckResponse(true, 0.92, 92.0, "CRITICAL"));
                when(fraudAlertRepository.existsByTransactionId(15L)).thenReturn(false);
                when(notificationRepository.existsByTransactionId(15L)).thenReturn(false);
                when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
                        FraudAlert alert = invocation.getArgument(0);
                        return TestEntities.withId(alert, 55L);
                });
                when(notificationRepository.existsByFraudAlertId(55L)).thenReturn(false);
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("149.62"), "Suspicious purchase", null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 8));

                transactionService.create(OWNER_ID, request);

                verify(fraudAlertRepository).save(any(FraudAlert.class));
                verify(notificationRepository).save(any(com.pulsewallet.pulsewallet.entity.Notification.class));
        }

        @Test
        void create_doesNotDuplicateAnExistingFraudAlert() {
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 16L);
                });
                when(fraudDetectionService.checkFraud(any()))
                                .thenReturn(new FraudCheckResponse(true, 0.92, 92.0, "CRITICAL"));
                when(fraudAlertRepository.existsByTransactionId(16L)).thenReturn(true);
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("149.62"), "Repeated suspicious purchase", null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 8));

                transactionService.create(OWNER_ID, request);

                verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
        }

        @Test
        void update_autoCategorizesAnExpenseWhenNoCategoryIsProvided() {
                Transaction existing = TestEntities.withId(
                                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 1)),
                                20L);
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
                                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 1)),
                                21L);
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
        void update_autoCategorizesIncomeWhenNoCategoryIsProvided() {
                Transaction existing = TestEntities.withId(
                                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                                                TransactionType.INCOME, LocalDate.of(2026, 8, 1)),
                                22L);
                Category salary = TestEntities.withId(
                                new Category("Salary", TransactionType.INCOME, null), 1L);
                when(transactionRepository.findByIdAndUserId(22L, OWNER_ID)).thenReturn(Optional.of(existing));
                when(incomeCategorizationService.categorize("Updated salary", OWNER_ID)).thenReturn(salary);
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("60000.00"), "Updated salary", null,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.update(22L, OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(1L);
                assertThat(response.categoryName()).isEqualTo("Salary");
                verify(incomeCategorizationService).categorize("Updated salary", OWNER_ID);
                verify(expenseCategorizationService, never()).categorize(any(), any());
        }

        @Test
        void update_preservesExplicitCategoryForIncomeTransaction() {
                Transaction existing = TestEntities.withId(
                                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                                                TransactionType.INCOME, LocalDate.of(2026, 8, 1)),
                                28L);
                Category freelance = TestEntities.withId(
                                new Category("Freelance", TransactionType.INCOME, null), 2L);
                when(transactionRepository.findByIdAndUserId(28L, OWNER_ID)).thenReturn(Optional.of(existing));
                when(categoryRepository.findById(2L)).thenReturn(Optional.of(freelance));
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("30000.00"), "Salary credited", 2L,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.update(28L, OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(2L);
                assertThat(response.categoryName()).isEqualTo("Freelance");
                verify(incomeCategorizationService, never()).categorize(any(), any());
        }

        @Test
        void update_handlesNullDescriptionForIncomeSafely() {
                Transaction existing = TestEntities.withId(
                                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                                                TransactionType.INCOME, LocalDate.of(2026, 8, 1)),
                                29L);
                Category otherIncome = TestEntities.withId(
                                new Category("Other Income", TransactionType.INCOME, null), 4L);
                when(transactionRepository.findByIdAndUserId(29L, OWNER_ID)).thenReturn(Optional.of(existing));
                when(incomeCategorizationService.categorize(null, OWNER_ID)).thenReturn(otherIncome);
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("400.00"), null, null,
                                TransactionType.INCOME, LocalDate.of(2026, 8, 1));

                TransactionResponse response = transactionService.update(29L, OWNER_ID, request);

                assertThat(response.description()).isNull();
                assertThat(response.categoryId()).isEqualTo(4L);
                assertThat(response.categoryName()).isEqualTo("Other Income");
                verify(incomeCategorizationService).categorize(null, OWNER_ID);
        }

        @Test
        void create_preservesExplicitCategoryEvenIfDescriptionContainsOtherKeywords() {
                Category healthcare = TestEntities.withId(
                                new Category("Healthcare", TransactionType.EXPENSE, null), 6L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(categoryRepository.findById(6L)).thenReturn(Optional.of(healthcare));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 23L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("2500.00"), "Amazon clothes", 6L,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 10));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(6L);
                assertThat(response.categoryName()).isEqualTo("Healthcare");
                verify(expenseCategorizationService, never()).categorize(any(), eq(OWNER_ID));
        }

        @Test
        void create_handlesNullDescriptionSafely() {
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(expenseCategorizationService.categorize(null, OWNER_ID)).thenReturn(null);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 24L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("100.00"), null, null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 10));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.description()).isNull();
                assertThat(response.categoryId()).isNull();
                verify(expenseCategorizationService).categorize(null, OWNER_ID);
        }

        @Test
        void create_swiggyDinnerAutoCategorizesToFoodAndDining() {
                Category foodAndDining = TestEntities.withId(
                                new Category("Food & Dining", TransactionType.EXPENSE, null), 2L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(expenseCategorizationService.categorize("Swiggy dinner", OWNER_ID))
                                .thenReturn(foodAndDining);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 25L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("450.00"), "Swiggy dinner", null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 10));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(2L);
                assertThat(response.categoryName()).isEqualTo("Food & Dining");
                verify(expenseCategorizationService).categorize("Swiggy dinner", OWNER_ID);
        }

        @Test
        void create_unknownExpenseFallsBackToOtherExpense() {
                Category otherExpense = TestEntities.withId(
                                new Category("Other Expense", TransactionType.EXPENSE, null), 10L);
                when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
                when(expenseCategorizationService.categorize("Unknown local purchase", OWNER_ID))
                                .thenReturn(otherExpense);
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                        Transaction saved = invocation.getArgument(0);
                        return TestEntities.withId(saved, 26L);
                });
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("75.00"), "Unknown local purchase", null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 10));

                TransactionResponse response = transactionService.create(OWNER_ID, request);

                assertThat(response.categoryId()).isEqualTo(10L);
                assertThat(response.categoryName()).isEqualTo("Other Expense");
                verify(expenseCategorizationService).categorize("Unknown local purchase", OWNER_ID);
        }

        @Test
        void update_handlesNullDescriptionSafely() {
                Transaction existing = TestEntities.withId(
                                new Transaction(owner, new BigDecimal("100.00"), "Old desc", null,
                                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 1)),
                                27L);
                when(transactionRepository.findByIdAndUserId(27L, OWNER_ID)).thenReturn(Optional.of(existing));
                when(expenseCategorizationService.categorize(null, OWNER_ID)).thenReturn(null);
                TransactionRequest request = new TransactionRequest(
                                new BigDecimal("120.00"), null, null,
                                TransactionType.EXPENSE, LocalDate.of(2026, 8, 5));

                TransactionResponse response = transactionService.update(27L, OWNER_ID, request);

                assertThat(response.description()).isNull();
                assertThat(response.categoryId()).isNull();
                verify(expenseCategorizationService).categorize(null, OWNER_ID);
        }
}
