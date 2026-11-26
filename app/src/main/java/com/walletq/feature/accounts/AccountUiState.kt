package com.walletq.feature.accounts

import com.walletq.core.model.Account
import com.walletq.core.model.AccountType
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.feature.activity.OwnershipFilter

enum class ActiveAccountSheet {
    NONE,
    ADD_OR_EDIT,
    DETAILS,
    ADJUST_BALANCE
}

data class CurrencyBalanceSummary(
    val currency: Currency,
    val totalMinorUnits: Long,
    val accountCount: Int
)

data class AccountFilterState(
    val searchQuery: String = "",
    val ownershipFilter: OwnershipFilter = OwnershipFilter.ALL,
    val selectedType: AccountType? = null,
    val showArchived: Boolean = false
)

data class AccountFormState(
    val editingAccountId: String? = null,
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val currencyCode: String = Currency.DEFAULT_REPORTING_CURRENCY.code,
    val ownershipType: OwnershipType = OwnershipType.PERSONAL,
    val selectedHouseholdId: String? = null,
    val openingBalanceInput: String = "0",
    val openingBalanceDateEpochMs: Long = System.currentTimeMillis(),
    val description: String = "",
    val iconIdentifier: String? = null,
    val colorIdentifier: String? = null,
    val validationError: String? = null,
    val isSubmitting: Boolean = false
) {
    val isEditing: Boolean get() = editingAccountId != null
}

data class BalanceAdjustmentFormState(
    val accountId: String = "",
    val accountName: String = "",
    val currency: Currency = Currency.DEFAULT_REPORTING_CURRENCY,
    val currentBalanceMinorUnits: Long = 0L,
    val targetBalanceInput: String = "",
    val reason: String = "",
    val validationError: String? = null,
    val isSubmitting: Boolean = false
)

data class AccountsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val accounts: List<Account> = emptyList(),
    val personalAccounts: List<Account> = emptyList(),
    val householdAccounts: List<Account> = emptyList(),
    val archivedAccounts: List<Account> = emptyList(),
    val currencySummaries: List<CurrencyBalanceSummary> = emptyList(),
    val households: List<Household> = emptyList(),
    val householdsById: Map<String, Household> = emptyMap(),
    val activeProfileId: String = "default-personal",
    val defaultCurrencyCode: String = Currency.DEFAULT_REPORTING_CURRENCY.code,
    val filterState: AccountFilterState = AccountFilterState(),
    val selectedAccount: Account? = null,
    val selectedAccountTransactions: List<Transaction> = emptyList(),
    val selectedAccountLedgerEntries: List<LedgerEntry> = emptyList(),
    val selectedAccountLedgerBalanceMinorUnits: Long? = null,
    val activeSheet: ActiveAccountSheet = ActiveAccountSheet.NONE,
    val snackbarMessage: String? = null
)
