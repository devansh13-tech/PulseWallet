package com.pulsewallet.pulsewallet.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.entity.Category;
import com.pulsewallet.pulsewallet.repository.CategoryRepository;

/**
 * Rule-based expense categorization. Given a transaction description, returns
 * the best-matching {@link Category} using keyword rules. When no rule matches,
 * falls back to the system-default "Other Expense" category.
 *
 * <p>Only called for EXPENSE transactions that have no explicit category set.
 */
@Service
public class ExpenseCategorizationService {

    private static final String OTHER_EXPENSE = "Other Expense";

    private final CategoryRepository categoryRepository;

    public ExpenseCategorizationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Categorize a transaction description into an expense category.
     *
     * @param description the transaction description (may be null)
     * @param userId      the user id, used to include user-created categories
     * @return the matched category, "Other Expense" if no rule matched, or
     *         {@code null} if the description itself is null
     */
    public Category categorize(String description, Long userId) {

        if (description == null) {
            return null;
        }
        description = description.trim().toLowerCase();

        List<Category> categories = categoryRepository.findByUserIdOrUserIsNull(userId);

        for (Category category : categories) {
            String categoryName = category.getName().toLowerCase();

            if (categoryName.equals("groceries") &&
                    (description.contains("milk")
                            || description.contains("grocery")
                            || description.contains("supermarket"))) {
                return category;
            }
            if (categoryName.equals("utilities") &&
                    (description.contains("electricity")
                            || description.contains("water")
                            || description.contains("gas")
                            || description.contains("internet")
                            || description.contains("broadband"))) {
                return category;
            }
            if (categoryName.equals("shopping") &&
                    (description.contains("amazon")
                            || description.contains("flipkart")
                            || description.contains("clothes")
                            || description.contains("shopping"))) {
                return category;
            }
            if (categoryName.equals("food & dining") &&
                    (description.contains("restaurant")
                            || description.contains("swiggy")
                            || description.contains("zomato")
                            || description.contains("food")
                            || description.contains("dinner")
                            || description.contains("lunch"))) {
                return category;
            }

            if (categoryName.equals("healthcare") &&
                    (description.contains("hospital")
                            || description.contains("doctor")
                            || description.contains("medicine")
                            || description.contains("pharmacy")
                            || description.contains("medical"))) {
                return category;
            }

            if (categoryName.equals("education") &&
                    (description.contains("college")
                            || description.contains("school")
                            || description.contains("course")
                            || description.contains("books")
                            || description.contains("tuition"))) {
                return category;
            }

            if (categoryName.equals("rent") ||
                    categoryName.equals("housing")) {

                if (description.contains("rent")
                        || description.contains("house rent")
                        || description.contains("landlord")) {
                    return category;
                }
            }

            if (categoryName.equals("entertainment") &&
                    (description.contains("movie")
                            || description.contains("cinema")
                            || description.contains("netflix")
                            || description.contains("spotify")
                            || description.contains("game"))) {
                return category;
            }

            if (categoryName.equals("transportation") &&
                    (description.contains("uber")
                            || description.contains("ola")
                            || description.contains("taxi")
                            || description.contains("bus")
                            || description.contains("train"))) {
                return category;
            }

            if (description.contains(categoryName)) {
                return category;
            }
        }

        // No keyword rule matched — fall back to the system-default "Other Expense".
        return categoryRepository.findByNameAndUserIsNull(OTHER_EXPENSE).orElse(null);
    }
}