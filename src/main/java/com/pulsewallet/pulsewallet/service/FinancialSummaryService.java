package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.AmountByCategory;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.AmountByDate;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.CategoryMonthlyAmount;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.MonthlyAmount;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@Service
public class FinancialSummaryService {

    private static final LocalDate EARLIEST_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST_DATE = LocalDate.of(9999, 12, 31);

    private final TransactionRepository transactionRepository;

    public FinancialSummaryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse summarize(Long userId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : EARLIEST_DATE;
        LocalDate effectiveTo = to != null ? to : LATEST_DATE;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        BigDecimal income = total(userId, TransactionType.INCOME, effectiveFrom, effectiveTo);
        BigDecimal expenses = total(userId, TransactionType.EXPENSE, effectiveFrom, effectiveTo);
        return new FinancialSummaryResponse(
                from,
                to,
                income,
                expenses,
                income.subtract(expenses),
                periodAmounts(userId, TransactionType.INCOME, effectiveFrom, effectiveTo),
                periodAmounts(userId, TransactionType.EXPENSE, effectiveFrom, effectiveTo),
                categoryAmounts(userId, effectiveFrom, effectiveTo),
                monthlyAmounts(userId, effectiveFrom, effectiveTo),
                categoryMonthlyAmounts(userId, effectiveFrom, effectiveTo));
    }

    private BigDecimal total(Long userId, TransactionType type, LocalDate from, LocalDate to) {
        BigDecimal total = transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(userId, type, from, to);
        return total != null ? total : BigDecimal.ZERO;
    }

    private List<AmountByDate> periodAmounts(Long userId, TransactionType type, LocalDate from, LocalDate to) {
        return transactionRepository.sumByPeriod(userId, type, from, to).stream()
                .map(row -> new AmountByDate(row.getPeriod(), row.getTotal()))
                .toList();
    }

    private List<AmountByCategory> categoryAmounts(Long userId, LocalDate from, LocalDate to) {
        return transactionRepository.sumExpensesByCategory(userId, from, to).stream()
                .map(row -> new AmountByCategory(row.getCategoryId(), row.getCategoryName(), row.getTotal()))
                .toList();
    }

    private List<MonthlyAmount> monthlyAmounts(Long userId, LocalDate from, LocalDate to) {
        return transactionRepository.sumByMonth(userId, from, to).stream()
                .map(row -> new MonthlyAmount(row.getYear(), row.getMonth(), row.getTotal()))
                .toList();
    }

    private List<CategoryMonthlyAmount> categoryMonthlyAmounts(Long userId, LocalDate from, LocalDate to) {
        return transactionRepository.sumExpensesByCategoryAndMonth(userId, from, to).stream()
                .map(row -> new CategoryMonthlyAmount(
                        row.getYear(), row.getMonth(), row.getCategoryId(), row.getCategoryName(), row.getTotal()))
                .toList();
    }
}
