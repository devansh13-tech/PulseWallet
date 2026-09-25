package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.FinancialGoalRequest;
import com.pulsewallet.pulsewallet.dto.FinancialGoalResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository financialGoalRepository;
    private final UserRepository userRepository;
    private final FinancialSummaryService financialSummaryService;
    private final Clock clock;

    public FinancialGoalService(FinancialGoalRepository financialGoalRepository, UserRepository userRepository,
            FinancialSummaryService financialSummaryService, Clock clock) {
        this.financialGoalRepository = financialGoalRepository;
        this.userRepository = userRepository;
        this.financialSummaryService = financialSummaryService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> list(Long userId) {
        BigDecimal monthlyLeft = monthlyLeft(userId);
        LocalDate today = LocalDate.now(clock);
        return financialGoalRepository.findByUserIdOrderByTargetDateAsc(userId).stream()
                .map(goal -> FinancialGoalResponse.from(goal, today, monthlyLeft)).toList();
    }

    @Transactional(readOnly = true)
    public FinancialGoalResponse get(Long id, Long userId) {
        return FinancialGoalResponse.from(requireOwned(id, userId), LocalDate.now(clock), monthlyLeft(userId));
    }

    @Transactional
    public FinancialGoalResponse create(Long userId, FinancialGoalRequest request) {
        validate(request, true);
        User user = userRepository.getReferenceById(userId);
        FinancialGoal goal = new FinancialGoal(user, request.name().trim(), request.goalType(), request.targetAmount(),
                request.currentAmount(), request.targetDate(), cleanDescription(request.description()));
        return FinancialGoalResponse.from(financialGoalRepository.save(goal), LocalDate.now(clock),
                monthlyLeft(userId));
    }

    @Transactional
    public FinancialGoalResponse update(Long id, Long userId, FinancialGoalRequest request) {
        validate(request, false);
        FinancialGoal goal = requireOwned(id, userId);
        goal.setName(request.name().trim());
        goal.setGoalType(request.goalType());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setTargetDate(request.targetDate());
        goal.setDescription(cleanDescription(request.description()));
        return FinancialGoalResponse.from(goal, LocalDate.now(clock), monthlyLeft(userId));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        financialGoalRepository.delete(requireOwned(id, userId));
    }

    private FinancialGoal requireOwned(Long id, Long userId) {
        return financialGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial goal", id));
    }

    private void validate(FinancialGoalRequest request, boolean creating) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.goalType() == null || request.targetAmount() == null
                || request.currentAmount() == null || request.targetDate() == null) {
            throw new IllegalArgumentException("goal name, type, amounts, and targetDate are required");
        }
        if (request.currentAmount().compareTo(request.targetAmount()) > 0) {
            throw new IllegalArgumentException("currentAmount must not exceed targetAmount");
        }
        if (creating && !request.targetDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("targetDate must be in the future");
        }
        if (!creating && request.currentAmount().compareTo(request.targetAmount()) < 0
                && !request.targetDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("targetDate must be in the future for an incomplete goal");
        }
    }

    private BigDecimal monthlyLeft(Long userId) {
        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, null, null);
        java.util.Set<String> months = new java.util.HashSet<>();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (FinancialSummaryResponse.MonthlyTypeAmount row : summary.monthlyIncomeExpense()) {
            months.add(row.year() + "-" + row.month());
            if (row.type() == com.pulsewallet.pulsewallet.entity.TransactionType.INCOME)
                income = income.add(value(row.total()));
            if (row.type() == com.pulsewallet.pulsewallet.entity.TransactionType.EXPENSE)
                expenses = expenses.add(value(row.total()));
        }
        return months.isEmpty() ? BigDecimal.ZERO
                : income.subtract(expenses).divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String cleanDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}