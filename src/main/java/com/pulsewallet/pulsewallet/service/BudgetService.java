package com.pulsewallet.pulsewallet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.BudgetRequest;
import com.pulsewallet.pulsewallet.dto.BudgetResponse;
import com.pulsewallet.pulsewallet.entity.Budget;
import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.BudgetRepository;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

/**
 * Plain CRUD over {@link Budget}: a limit, an optional category, and a date
 * range, scoped to the authenticated user. This is deliberately just the
 * Milestone 2 data model - the Milestone 3 engine that calculates what the
 * amount *should* be (salary minus expenses, forecasting, advisory) is not
 * implemented here.
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        return budgetRepository.findByUserId(userId).stream()
                .map(BudgetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse get(Long id, Long userId) {
        return BudgetResponse.from(requireOwned(id, userId));
    }

    @Transactional
    public BudgetResponse create(Long userId, BudgetRequest request) {
        validateRange(request);
        User user = userRepository.getReferenceById(userId);
        Category category = resolveCategory(request.categoryId(), userId);
        Budget budget = new Budget(user, category, request.amount(), request.startDate(), request.endDate());
        return BudgetResponse.from(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse update(Long id, Long userId, BudgetRequest request) {
        validateRange(request);
        Budget budget = requireOwned(id, userId);
        Category category = resolveCategory(request.categoryId(), userId);
        budget.setAmount(request.amount());
        budget.setCategory(category);
        budget.setStartDate(request.startDate());
        budget.setEndDate(request.endDate());
        return BudgetResponse.from(budget);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        budgetRepository.delete(requireOwned(id, userId));
    }

    private Budget requireOwned(Long id, Long userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    /** Cross-field rule Bean Validation cannot express on its own: the end must not precede the start. */
    private void validateRange(BudgetRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

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
}
