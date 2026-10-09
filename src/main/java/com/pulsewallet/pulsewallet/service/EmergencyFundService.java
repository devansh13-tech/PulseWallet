package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse;
import com.pulsewallet.pulsewallet.dto.EmergencyFundPlanResponse.EssentialExpenseCategory;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.AmountByCategory;
import com.pulsewallet.pulsewallet.dto.FinancialSummaryResponse.CategoryMonthlyAmount;
import com.pulsewallet.pulsewallet.entity.FinancialGoal;
import com.pulsewallet.pulsewallet.entity.FinancialGoalType;
import com.pulsewallet.pulsewallet.entity.InvestmentProfile;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.InvestmentProfileRepository;

@Service
public class EmergencyFundService {

    public static final int DEFAULT_RECOMMENDED_MONTHS = 6;
    public static final BigDecimal DEFAULT_CONTRIBUTION_PERCENT = new BigDecimal("0.20"); // 20% of disposable income
    public static final int SCALE = 2;

    public static final Set<String> ESSENTIAL_CATEGORIES = Set.of(
            "rent",
            "utilities",
            "groceries",
            "healthcare",
            "education",
            "transportation");

    private final FinancialSummaryService financialSummaryService;
    private final FinancialGoalRepository financialGoalRepository;
    private final InvestmentProfileRepository investmentProfileRepository;

    public EmergencyFundService(
            FinancialSummaryService financialSummaryService,
            FinancialGoalRepository financialGoalRepository,
            InvestmentProfileRepository investmentProfileRepository) {
        this.financialSummaryService = financialSummaryService;
        this.financialGoalRepository = financialGoalRepository;
        this.investmentProfileRepository = investmentProfileRepository;
    }

    @Transactional(readOnly = true)
    public EmergencyFundPlanResponse calculate(Long userId) {
        return calculate(userId, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public EmergencyFundPlanResponse calculate(Long userId, LocalDate from, LocalDate to) {
        return calculate(userId, from, to, null, null);
    }

    @Transactional(readOnly = true)
    public EmergencyFundPlanResponse calculate(
            Long userId,
            LocalDate from,
            LocalDate to,
            BigDecimal customCurrentFund,
            Integer customMonths) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        FinancialSummaryResponse summary = financialSummaryService.summarize(userId, from, to);

        // 1. Calculate monthly essential expenses
        List<EssentialExpenseCategory> categoryList = new ArrayList<>();
        BigDecimal monthlyEssential = calculateMonthlyEssentialExpenses(summary, categoryList);

        // 2. Recommended emergency-fund months
        int recommendedMonths = determineRecommendedMonths(userId, customMonths);

        // 3. Recommended emergency-fund target
        BigDecimal target = monthlyEssential.multiply(BigDecimal.valueOf(recommendedMonths))
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 4. Current emergency-fund progress
        BigDecimal currentFund = resolveCurrentEmergencyFund(userId, customCurrentFund);

        // 5. Remaining amount needed
        BigDecimal remaining = target.subtract(currentFund).max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 6. Progress percentage
        BigDecimal progress;
        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            progress = new BigDecimal("100.00");
        } else {
            progress = currentFund.multiply(new BigDecimal("100"))
                    .divide(target, SCALE, RoundingMode.HALF_UP);
        }

        // 7. Suggested monthly contribution based on disposable income
        BigDecimal disposable = summary.disposableIncome() != null ? summary.disposableIncome() : BigDecimal.ZERO;
        BigDecimal suggestedContribution = calculateSuggestedContribution(disposable, remaining);

        return new EmergencyFundPlanResponse(
                from,
                to,
                monthlyEssential,
                recommendedMonths,
                target,
                currentFund,
                remaining,
                suggestedContribution,
                disposable,
                progress,
                categoryList);
    }

    public boolean isEssential(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return false;
        }
        return ESSENTIAL_CATEGORIES.contains(categoryName.trim().toLowerCase(Locale.ROOT));
    }

    private BigDecimal calculateMonthlyEssentialExpenses(
            FinancialSummaryResponse summary, List<EssentialExpenseCategory> categoryList) {
        if (summary == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        // Prefer monthly breakdown if available for accurate normalization
        if (summary.categoryMonthlyTotals() != null && !summary.categoryMonthlyTotals().isEmpty()) {
            BigDecimal totalEssential = BigDecimal.ZERO;
            Set<String> distinctMonths = new HashSet<>();
            Map<String, BigDecimal> categorySums = new LinkedHashMap<>();

            for (CategoryMonthlyAmount row : summary.categoryMonthlyTotals()) {
                if (row.categoryName() != null && isEssential(row.categoryName())) {
                    String catName = row.categoryName().trim();
                    BigDecimal amount = row.total() != null ? row.total() : BigDecimal.ZERO;
                    categorySums.merge(catName, amount, BigDecimal::add);
                    totalEssential = totalEssential.add(amount);
                }
                distinctMonths.add(row.year() + "-" + row.month());
            }

            int monthCount = Math.max(distinctMonths.size(), 1);
            for (Map.Entry<String, BigDecimal> entry : categorySums.entrySet()) {
                BigDecimal monthlyCat = entry.getValue()
                        .divide(BigDecimal.valueOf(monthCount), SCALE, RoundingMode.HALF_UP)
                        .max(BigDecimal.ZERO);
                categoryList.add(new EssentialExpenseCategory(entry.getKey(), monthlyCat));
            }

            return totalEssential.divide(BigDecimal.valueOf(monthCount), SCALE, RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO);
        }

        // Fallback to expensesByCategory
        if (summary.expensesByCategory() != null) {
            BigDecimal totalEssential = BigDecimal.ZERO;
            for (AmountByCategory row : summary.expensesByCategory()) {
                if (row.categoryName() != null && isEssential(row.categoryName())) {
                    BigDecimal amount = row.total() != null ? row.total() : BigDecimal.ZERO;
                    totalEssential = totalEssential.add(amount);
                    categoryList.add(new EssentialExpenseCategory(
                            row.categoryName().trim(),
                            amount.max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP)));
                }
            }
            return totalEssential.max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private int determineRecommendedMonths(Long userId, Integer customMonths) {
        if (customMonths != null && customMonths > 0) {
            return customMonths;
        }

        if (investmentProfileRepository != null) {
            InvestmentProfile profile = investmentProfileRepository.findByUserId(userId).orElse(null);
            if (profile != null && profile.getRiskPreference() != null) {
                return switch (profile.getRiskPreference()) {
                    case CONSERVATIVE -> 6;
                    case MODERATE -> 6;
                    case AGGRESSIVE -> 3;
                };
            }
        }

        return DEFAULT_RECOMMENDED_MONTHS;
    }

    private BigDecimal resolveCurrentEmergencyFund(Long userId, BigDecimal customCurrentFund) {
        if (customCurrentFund != null) {
            return customCurrentFund.max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal fund = BigDecimal.ZERO;
        if (financialGoalRepository != null) {
            List<FinancialGoal> goals = financialGoalRepository.findByUserIdOrderByTargetDateAsc(userId);
            for (FinancialGoal goal : goals) {
                if (goal.getGoalType() == FinancialGoalType.EMERGENCY_FUND && goal.getCurrentAmount() != null) {
                    fund = fund.add(goal.getCurrentAmount().max(BigDecimal.ZERO));
                }
            }
        }

        return fund.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSuggestedContribution(BigDecimal disposable, BigDecimal remainingTarget) {
        if (disposable == null || disposable.compareTo(BigDecimal.ZERO) <= 0
                || remainingTarget == null || remainingTarget.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal fromDisposable = disposable.multiply(DEFAULT_CONTRIBUTION_PERCENT)
                .setScale(SCALE, RoundingMode.HALF_UP);

        return fromDisposable.min(remainingTarget).max(BigDecimal.ZERO);
    }
}
