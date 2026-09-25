package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.ForecastResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.MonthlyTypeTotalProjection;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@Service
public class ForecastService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    @Autowired
    public ForecastService(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public ForecastService(TransactionRepository transactionRepository) {
        this(transactionRepository, Clock.systemDefaultZone());
    }

    @Transactional(readOnly = true)
    public ForecastResponse forecast(Long userId) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusMonths(12).withDayOfMonth(1);

        List<MonthlyTypeTotalProjection> monthlyTotals = transactionRepository.sumByMonthAndType(userId, from, to);
        List<ForecastResponse.MonthlyExpensePoint> expensePoints = new ArrayList<>();
        BigDecimal totalExpense = ZERO;

        for (MonthlyTypeTotalProjection row : monthlyTotals) {
            if (row.getType() == TransactionType.EXPENSE) {
                BigDecimal expense = row.getTotal() != null ? row.getTotal() : ZERO;
                expensePoints.add(new ForecastResponse.MonthlyExpensePoint(row.getYear(), row.getMonth(), expense));
                totalExpense = totalExpense.add(expense);
            }
        }

        int monthsIncluded = expensePoints.size();
        BigDecimal averageMonthlyExpense = monthsIncluded == 0 ? ZERO
                : totalExpense.divide(
                        BigDecimal.valueOf(monthsIncluded), MONEY_SCALE, RoundingMode.HALF_UP);

        return new ForecastResponse(
                monthsIncluded,
                averageMonthlyExpense.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                averageMonthlyExpense.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                "average of the last available monthly expense totals for this user.",
                expensePoints);
    }
}
