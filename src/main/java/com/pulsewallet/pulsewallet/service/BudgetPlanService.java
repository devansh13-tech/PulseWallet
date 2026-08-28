package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.BudgetPlanRequest;
import com.pulsewallet.pulsewallet.dto.BudgetPlanResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@Service
public class BudgetPlanService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal SAVINGS_PERCENT = new BigDecimal("0.20");
    private static final BigDecimal INVESTMENT_PERCENT = new BigDecimal("0.10");
    private static final int MONEY_SCALE = 2;

    private final TransactionRepository transactionRepository;

    public BudgetPlanService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public BudgetPlanResponse createPlan(Long userId, BudgetPlanRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        BigDecimal salary = request.salary();
        if (salary == null || salary.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("salary must be greater than 0");
        }
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }

        BigDecimal totalExpenses = totalExpenses(userId, startDate, endDate);
        BigDecimal disposableIncome = salary.subtract(totalExpenses);

        BigDecimal recommendedSavings = calculatePercentage(disposableIncome, SAVINGS_PERCENT);
        BigDecimal recommendedInvestment = calculatePercentage(disposableIncome, INVESTMENT_PERCENT);
        BigDecimal recommendedSpending = disposableIncome
                .subtract(recommendedSavings)
                .subtract(recommendedInvestment);

        if (disposableIncome.compareTo(ZERO) <= 0) {
            recommendedSavings = ZERO;
            recommendedInvestment = ZERO;
            recommendedSpending = ZERO;
        } else if (recommendedSpending.compareTo(ZERO) < 0) {
            recommendedSpending = ZERO;
        }

        return new BudgetPlanResponse(
                salary.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                totalExpenses.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                disposableIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                recommendedSavings.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                recommendedInvestment.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                recommendedSpending.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal totalExpenses(Long userId, LocalDate from, LocalDate to) {
        BigDecimal amount = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, from, to);
        return amount != null ? amount : ZERO;
    }

    private BigDecimal calculatePercentage(BigDecimal base, BigDecimal percent) {
        if (base == null || base.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return base.multiply(percent).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}