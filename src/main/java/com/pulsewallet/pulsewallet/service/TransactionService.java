package com.pulsewallet.pulsewallet.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;
import com.pulsewallet.pulsewallet.dto.TransactionRequest;
import com.pulsewallet.pulsewallet.dto.TransactionResponse;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.FraudAlert;
import com.pulsewallet.pulsewallet.entity.FraudAlert.RiskLevel;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.FraudAlertRepository;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

/**
 * CRUD over {@link Transaction}, always scoped to the authenticated user.
 * Every lookup goes through {@code findByIdAndUserId} or an explicit
 * ownership check, so a transaction ID belonging to another user resolves to
 * {@link ResourceNotFoundException} (404) rather than ever being readable.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseCategorizationService expenseCategorizationService;
    private final FraudDetectionService fraudDetectionService;
    private final FraudAlertRepository fraudAlertRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ExpenseCategorizationService expenseCategorizationService,
            FraudDetectionService fraudDetectionService,
            FraudAlertRepository fraudAlertRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.expenseCategorizationService = expenseCategorizationService;
        this.fraudDetectionService = fraudDetectionService;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Long userId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from == null && to == null) {
            return list(userId, pageable);
        }
        LocalDate effectiveFrom = from != null ? from : LocalDate.of(1900, 1, 1);
        LocalDate effectiveTo = to != null ? to : LocalDate.of(9999, 12, 31);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, effectiveFrom, effectiveTo, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id, Long userId) {
        return TransactionResponse.from(requireOwned(id, userId));
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        User user = userRepository.getReferenceById(userId);
        Category category = resolveCategory(request.categoryId(), userId);
        if (category == null && request.type() == TransactionType.EXPENSE) {
            category = expenseCategorizationService.categorize(request.description(), userId);
        }
        Transaction transaction = new Transaction(
                user,
                request.amount(),
                request.description(),
                request.merchant(),
                request.paymentChannel(),
                category,
                request.type(),
                request.transactionDate());
        Transaction saved = transactionRepository.save(transaction);
        triggerFraudCheckIfNeeded(saved, request);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(Long id, Long userId, TransactionRequest request) {
        Transaction transaction = requireOwned(id, userId);
        Category category = resolveCategory(request.categoryId(), userId);
        if (category == null && request.type() == TransactionType.EXPENSE) {
            category = expenseCategorizationService.categorize(request.description(), userId);
        }
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setMerchant(request.merchant());
        transaction.setPaymentChannel(request.paymentChannel());
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setTransactionDate(request.transactionDate());
        triggerFraudCheckIfNeeded(transaction, request);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        transactionRepository.delete(requireOwned(id, userId));
    }

    private Transaction requireOwned(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    /**
     * Null categoryId is allowed (uncategorized); a categoryId owned by someone
     * else is not.
     */
    private Category resolveCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        return category;
    }

    private void triggerFraudCheckIfNeeded(Transaction transaction, TransactionRequest request) {
        if (fraudDetectionService == null || transaction.getType() != TransactionType.EXPENSE) {
            return;
        }

        FraudCheckRequest fraudRequest = FraudCheckRequest.safeFallback(
                transaction.getTransactionDate(),
                transaction.getAmount().doubleValue());

        FraudCheckResponse response = fraudDetectionService.checkFraud(fraudRequest);
        if (response != null && response.isFraud()
                && !fraudAlertRepository.existsByTransactionId(transaction.getId())) {
            fraudAlertRepository.save(new FraudAlert(
                    transaction,
                    transaction.getUser(),
                    java.math.BigDecimal.valueOf(response.fraudProbability()),
                    java.math.BigDecimal.valueOf(response.riskScore()),
                    toRiskLevel(response.riskLevel())));
        }
    }

    private RiskLevel toRiskLevel(String riskLevel) {
        try {
            return RiskLevel.valueOf(riskLevel);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return RiskLevel.HIGH;
        }
    }
}
