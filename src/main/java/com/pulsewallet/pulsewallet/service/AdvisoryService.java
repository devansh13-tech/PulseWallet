package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.AdvisoryResponse;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.MonthlyTypeTotalProjection;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@Service
public class AdvisoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal EMERGENCY_FUND_MONTHS = new BigDecimal("6");
    private static final BigDecimal MEDICAL_RESERVE_MONTHS = new BigDecimal("3");
    private static final BigDecimal SAVINGS_PERCENT = new BigDecimal("0.30");
    private static final BigDecimal INVESTMENT_PERCENT = new BigDecimal("0.15");
    private static final int MONEY_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    @Autowired
    public AdvisoryService(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public AdvisoryService(TransactionRepository transactionRepository) {
        this(transactionRepository, Clock.systemDefaultZone());
    }

    @Transactional(readOnly = true)
    public AdvisoryResponse advice(Long userId) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusMonths(12).withDayOfMonth(1);

        List<MonthlyTypeTotalProjection> rows = transactionRepository.sumByMonthAndType(userId, from, to);
        BigDecimal monthlyIncome = ZERO;
        BigDecimal monthlyExpenses = ZERO;

        for (MonthlyTypeTotalProjection row : rows) {
            BigDecimal total = row.getTotal() != null ? row.getTotal() : ZERO;
            if (row.getType() == TransactionType.INCOME) {
                monthlyIncome = monthlyIncome.add(total);
            } else if (row.getType() == TransactionType.EXPENSE) {
                monthlyExpenses = monthlyExpenses.add(total);
            }
        }

        BigDecimal disposableIncome = monthlyIncome.subtract(monthlyExpenses);
        if (rows.isEmpty()) {
            disposableIncome = ZERO;
        }
        BigDecimal emergencyFundTarget = monthlyExpenses.multiply(EMERGENCY_FUND_MONTHS).setScale(MONEY_SCALE,
                RoundingMode.HALF_UP);
        BigDecimal shortTermReserveTarget = monthlyExpenses.multiply(MEDICAL_RESERVE_MONTHS).setScale(MONEY_SCALE,
                RoundingMode.HALF_UP);

        BigDecimal recommendedSavings = disposableIncome.compareTo(ZERO) > 0
                ? disposableIncome.multiply(SAVINGS_PERCENT).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;
        BigDecimal recommendedInvestment = disposableIncome.compareTo(ZERO) > 0
                ? disposableIncome.multiply(INVESTMENT_PERCENT).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : ZERO;

        String guidance = "illustrative allocation only; not personalised financial advice and not a regulated advisory claim.";

        return new AdvisoryResponse(
                monthlyIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                monthlyExpenses.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                disposableIncome.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                emergencyFundTarget,
                shortTermReserveTarget,
                recommendedSavings,
                recommendedInvestment,
                guidance);
    }
}
