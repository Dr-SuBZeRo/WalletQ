package com.walletq.core.model

import kotlinx.serialization.Serializable

/**
 * Domain entity representing an income or expense classification.
 *
 * Supports system defaults as well as personal and household user-defined categories.
 */
@Serializable
data class Category(
    val id: String,
    val name: String,
    val transactionType: TransactionType,
    val iconIdentifier: String,
    val colorIdentifier: String,
    val parentCategoryId: String? = null,
    val profileId: String? = null,
    val householdId: String? = null,
    val isSystemCategory: Boolean = false,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    companion object {
        // Predefined System Categories
        val DEFAULT_CATEGORIES = listOf(
            // Expense Categories
            Category("cat-food", "Food & Dining", TransactionType.EXPENSE, "restaurant", "#FF5722", isSystemCategory = true),
            Category("cat-groceries", "Groceries", TransactionType.EXPENSE, "shopping_cart", "#4CAF50", isSystemCategory = true),
            Category("cat-transport", "Transportation", TransactionType.EXPENSE, "directions_car", "#2196F3", isSystemCategory = true),
            Category("cat-housing", "Housing & Rent", TransactionType.EXPENSE, "home", "#795548", isSystemCategory = true),
            Category("cat-utilities", "Utilities & Bills", TransactionType.EXPENSE, "bolt", "#FF9800", isSystemCategory = true),
            Category("cat-health", "Health & Medical", TransactionType.EXPENSE, "medical_services", "#E91E63", isSystemCategory = true),
            Category("cat-entertainment", "Entertainment", TransactionType.EXPENSE, "movie", "#9C27B0", isSystemCategory = true),
            Category("cat-shopping", "Shopping", TransactionType.EXPENSE, "shopping_bag", "#00BCD4", isSystemCategory = true),
            Category("cat-personal", "Personal Care", TransactionType.EXPENSE, "spa", "#009688", isSystemCategory = true),
            Category("cat-education", "Education", TransactionType.EXPENSE, "school", "#3F51B5", isSystemCategory = true),
            Category("cat-fees", "Financial & Bank Fees", TransactionType.EXPENSE, "account_balance", "#607D8B", isSystemCategory = true),
            Category("cat-misc-expense", "Other Expense", TransactionType.EXPENSE, "more_horiz", "#9E9E9E", isSystemCategory = true),

            // Income Categories
            Category("cat-salary", "Salary", TransactionType.INCOME, "work", "#4CAF50", isSystemCategory = true),
            Category("cat-business", "Business & Freelance", TransactionType.INCOME, "business_center", "#2196F3", isSystemCategory = true),
            Category("cat-investments", "Investments & Dividends", TransactionType.INCOME, "trending_up", "#009688", isSystemCategory = true),
            Category("cat-gifts", "Gifts & Grants", TransactionType.INCOME, "card_giftcard", "#E91E63", isSystemCategory = true),
            Category("cat-misc-income", "Other Income", TransactionType.INCOME, "savings", "#FF9800", isSystemCategory = true)
        )
    }
}
