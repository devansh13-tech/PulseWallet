package com.pulsewallet.pulsewallet.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;

/**
 * Rule-based income categorization. Given a transaction description, returns
 * the best-matching {@link Category} using deterministic keyword rules. When no
 * rule matches, falls back to the system-default "Other Income" category.
 *
 * <p>Only called for INCOME transactions that have no explicit category set.
 */
@Service
public class IncomeCategorizationService {

    public static final String OTHER_INCOME = "Other Income";
    public static final String SALARY = "Salary";
    public static final String FREELANCE = "Freelance";
    public static final String INVESTMENT_RETURNS = "Investment Returns";

    private final CategoryRepository categoryRepository;

    public IncomeCategorizationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Categorize an income transaction description into an income category.
     *
     * @param description the transaction description (may be null)
     * @param userId      the user id, used to include user-created categories
     * @return the matched income category, falling back to "Other Income"
     */
    public Category categorize(String description, Long userId) {
        if (description == null || description.isBlank()) {
            return categoryRepository.findByNameAndUserIsNull(OTHER_INCOME).orElse(null);
        }

        String normalized = description.trim().toLowerCase();
        List<Category> categories = categoryRepository.findByUserIdOrUserIsNull(userId);

        for (Category category : categories) {
            // Category safety: only consider INCOME categories
            if (category.getType() != null && category.getType() != TransactionType.INCOME) {
                continue;
            }

            String categoryName = category.getName().toLowerCase();

            if (categoryName.equals("salary") && matchesSalary(normalized)) {
                return category;
            }

            if (categoryName.equals("freelance") && matchesFreelance(normalized)) {
                return category;
            }

            if ((categoryName.equals("investment returns") || categoryName.equals("investments"))
                    && matchesInvestmentReturns(normalized)) {
                return category;
            }

            // Custom user-defined income category matching by name
            if (!categoryName.equals("other income") && normalized.contains(categoryName)) {
                return category;
            }
        }

        // Fallback to system-default "Other Income"
        return categoryRepository.findByNameAndUserIsNull(OTHER_INCOME).orElse(null);
    }

    private boolean matchesSalary(String text) {
        return text.contains("salary")
                || text.contains("salary credited")
                || text.contains("monthly salary")
                || text.contains("payroll")
                || text.contains("payroll credit")
                || text.contains("employer")
                || text.contains("wages")
                || text.contains("compensation");
    }

    private boolean matchesFreelance(String text) {
        return text.contains("freelance")
                || text.contains("freelancing")
                || text.contains("client payment")
                || text.contains("client")
                || text.contains("project payment")
                || text.contains("consulting")
                || text.contains("contract payment");
    }

    private boolean matchesInvestmentReturns(String text) {
        return text.contains("dividend")
                || text.contains("dividend received")
                || text.contains("stock dividend")
                || text.contains("interest credited")
                || text.contains("interest income")
                || text.contains("bond interest")
                || text.contains("fd interest")
                || text.contains("interest")
                || text.contains("investment return")
                || text.contains("mutual fund return");
    }
}
