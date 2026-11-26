package com.walletq.core.model

enum class BudgetPeriod(val displayName: String) {
    MONTHLY("Monthly")
}

data class BudgetCategoryAllocation(
    val id: String,
    val budgetId: String,
    val categoryId: String,
    val allocatedAmountMinorUnits: Long
)

data class Budget(
    val id: String,
    val name: String,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val currency: Currency,
    val allocatedAmountMinorUnits: Long,
    val ownershipType: OwnershipType = OwnershipType.PERSONAL,
    val profileId: String? = null,
    val householdId: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val categoryAllocations: List<BudgetCategoryAllocation> = emptyList()
)

data class CategorySpendingSummary(
    val categoryId: String,
    val allocatedMinorUnits: Long,
    val actualSpendingMinorUnits: Long,
    val remainingMinorUnits: Long,
    val percentageConsumed: Int,
    val overBudgetMinorUnits: Long
)

data class BudgetSpendingSummary(
    val budget: Budget,
    val allocatedMinorUnits: Long,
    val actualSpendingMinorUnits: Long,
    val remainingMinorUnits: Long,
    val percentageConsumed: Int,
    val overBudgetMinorUnits: Long,
    val categorySummaries: List<CategorySpendingSummary>
)
