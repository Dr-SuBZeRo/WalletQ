package com.walletq.feature.activity

import com.walletq.core.model.Account
import com.walletq.core.model.Category
import com.walletq.core.model.Household
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionType

enum class OwnershipFilter(val label: String) {
    ALL("All"),
    PERSONAL("Personal"),
    HOUSEHOLD("Household")
}

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("7 Days"),
    THIS_MONTH("This Month")
}

enum class ActiveActivitySheet {
    NONE,
    ADD_TRANSACTION,
    TRANSFER,
    DETAILS
}

data class TransactionFilterState(
    val searchQuery: String = "",
    val ownershipFilter: OwnershipFilter = OwnershipFilter.ALL,
    val selectedType: TransactionType? = null,
    val selectedAccountId: String? = null,
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL
)

data class TransactionDateGroup(
    val epochDay: Long,
    val dateLabel: String,
    val transactions: List<Transaction>
)

data class TransactionFormState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val description: String = "",
    val notes: String = "",
    val ownershipContext: OwnershipType = OwnershipType.PERSONAL,
    val validationError: String? = null,
    val isSubmitting: Boolean = false
)

data class TransferFormState(
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val sourceAmountInput: String = "",
    val exchangeRateInput: String = "",
    val exchangeRateReferenceId: String? = null,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isCrossCurrency: Boolean = false,
    val destinationPreview: Money? = null,
    val validationError: String? = null,
    val isSubmitting: Boolean = false
)

data class ActivityUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val groupedTransactions: List<TransactionDateGroup> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val accountsById: Map<String, Account> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val categoriesById: Map<String, Category> = emptyMap(),
    val householdsById: Map<String, Household> = emptyMap(),
    val activeProfileId: String = "default-personal",
    val filterState: TransactionFilterState = TransactionFilterState(),
    val selectedTransaction: Transaction? = null,
    val selectedTransactionLedgerEntries: List<LedgerEntry> = emptyList(),
    val isReversing: Boolean = false,
    val activeSheet: ActiveActivitySheet = ActiveActivitySheet.NONE,
    val snackbarMessage: String? = null
)
