package com.walletq.feature.budgets

import com.walletq.core.model.BudgetSpendingSummary
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.OwnershipType
import com.walletq.feature.activity.OwnershipFilter
import java.time.YearMonth

data class CategoryAllocationDraft(
    val id: String,
    val categoryId: String,
    val amountInput: String
)

data class BudgetFilterState(
    val ownershipFilter: OwnershipFilter = OwnershipFilter.ALL,
    val showArchived: Boolean = false
)

data class BudgetFormState(
    val editingBudgetId: String? = null,
    val name: String = "",
    val yearMonth: YearMonth = YearMonth.now(),
    val currencyCode: String = Currency.DEFAULT_REPORTING_CURRENCY.code,
    val allocatedAmountInput: String = "",
    val ownershipType: OwnershipType = OwnershipType.PERSONAL,
    val selectedHouseholdId: String? = null,
    val categoryAllocations: List<CategoryAllocationDraft> = emptyList(),
    val validationError: String? = null,
    val isSubmitting: Boolean = false
) {
    val isEditing: Boolean get() = editingBudgetId != null
}

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val budgetSummaries: List<BudgetSpendingSummary> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val categoriesById: Map<String, Category> = emptyMap(),
    val households: List<Household> = emptyList(),
    val householdsById: Map<String, Household> = emptyMap(),
    val activeProfileId: String = "default-personal",
    val defaultCurrencyCode: String = Currency.DEFAULT_REPORTING_CURRENCY.code,
    val filterState: BudgetFilterState = BudgetFilterState(),
    val isFormSheetOpen: Boolean = false,
    val snackbarMessage: String? = null
)
