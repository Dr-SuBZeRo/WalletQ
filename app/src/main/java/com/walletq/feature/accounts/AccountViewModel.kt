package com.walletq.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.repository.AccountRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.datastore.PreferencesRepository
import com.walletq.core.model.Account
import com.walletq.core.model.AccountReconciliationEngine
import com.walletq.core.model.AccountType
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.LedgerEngine
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.feature.activity.MoneyInputParser
import com.walletq.feature.activity.OwnershipFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val householdDao: HouseholdDao,
    private val profileDao: ProfileDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(AccountFilterState())
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _activeSheet = MutableStateFlow(ActiveAccountSheet.NONE)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _streamError = MutableStateFlow<String?>(null)

    private val _accountFormState = MutableStateFlow(AccountFormState())
    val accountFormState: StateFlow<AccountFormState> = _accountFormState.asStateFlow()

    private val _balanceAdjustmentState = MutableStateFlow(BalanceAdjustmentFormState())
    val balanceAdjustmentState: StateFlow<BalanceAdjustmentFormState> = _balanceAdjustmentState.asStateFlow()

    private val selectedAccountTransactionsFlow: Flow<List<Transaction>> =
        _selectedAccountId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else transactionRepository.getTransactionsByAccount(id)
        }

    private val selectedAccountLedgerEntriesFlow: Flow<List<LedgerEntry>> =
        _selectedAccountId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else transactionRepository.getLedgerEntriesForAccount(id)
        }

    private data class CoreAccountData(
        val allAccounts: List<Account>,
        val households: List<Household>,
        val activeProfileId: String,
        val defaultCurrencyCode: String
    )

    private val coreDataFlow: Flow<CoreAccountData> = combine(
        accountRepository.observeAllAccounts(),
        householdDao.getAllHouseholdsFlow().map { list -> list.map { it.toDomain() } },
        profileDao.getDefaultProfileFlow().map { it?.id ?: "default-personal" },
        preferencesRepository.userPreferencesFlow.map { it.defaultCurrencyCode }
    ) { accounts, households, profileId, defaultCurrency ->
        CoreAccountData(
            allAccounts = accounts,
            households = households,
            activeProfileId = profileId,
            defaultCurrencyCode = defaultCurrency
        )
    }

    private data class UiMetaBundle(
        val filterState: AccountFilterState,
        val selectedAccountId: String?,
        val selectedTransactions: List<Transaction>,
        val selectedLedgerEntries: List<LedgerEntry>,
        val activeSheet: ActiveAccountSheet,
        val snackbarMessage: String?,
        val streamError: String?
    )

    private val uiMetaFlow: Flow<UiMetaBundle> = combine(
        _filterState,
        _selectedAccountId,
        selectedAccountTransactionsFlow,
        selectedAccountLedgerEntriesFlow,
        _activeSheet,
        _snackbarMessage,
        _streamError
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        UiMetaBundle(
            filterState = values[0] as AccountFilterState,
            selectedAccountId = values[1] as String?,
            selectedTransactions = values[2] as List<Transaction>,
            selectedLedgerEntries = values[3] as List<LedgerEntry>,
            activeSheet = values[4] as ActiveAccountSheet,
            snackbarMessage = values[5] as String?,
            streamError = values[6] as String?
        )
    }

    val uiState: StateFlow<AccountsUiState> = combine(coreDataFlow, uiMetaFlow) { data, meta ->
        val filtered = applyFilters(data.allAccounts, meta.filterState)
        val activeFiltered = filtered.filter { !it.isArchived }
        val personalAccounts = activeFiltered.filter { it.ownershipType == OwnershipType.PERSONAL }
        val householdAccounts = activeFiltered.filter { it.ownershipType == OwnershipType.HOUSEHOLD }
        val archivedAccounts = filtered.filter { it.isArchived }

        // Group totals strictly by currency; never sum unlike currencies together.
        val activeAll = data.allAccounts.filter { !it.isArchived }
        val currencySummaries = activeAll
            .groupBy { it.currency.code }
            .values
            .mapNotNull { group ->
                val currency = group.firstOrNull()?.currency ?: return@mapNotNull null
                val totalMinor = group.fold(0L) { acc, account ->
                    Math.addExact(acc, account.currentBalanceMinorUnits)
                }
                CurrencyBalanceSummary(
                    currency = currency,
                    totalMinorUnits = totalMinor,
                    accountCount = group.size
                )
            }
            .sortedBy { it.currency.code }

        val selectedAccount = meta.selectedAccountId?.let { id ->
            data.allAccounts.find { it.id == id }
        }
        val reconciliationReport = selectedAccount?.let { account ->
            AccountReconciliationEngine.reconcile(
                account = account,
                transactionsForAccount = meta.selectedTransactions,
                ledgerEntriesForAccount = meta.selectedLedgerEntries
            )
        }
        val ledgerDerivedBalance = reconciliationReport?.expectedBalanceMinorUnits

        AccountsUiState(
            isLoading = false,
            errorMessage = meta.streamError,
            accounts = filtered,
            personalAccounts = personalAccounts,
            householdAccounts = householdAccounts,
            archivedAccounts = archivedAccounts,
            currencySummaries = currencySummaries,
            households = data.households,
            householdsById = data.households.associateBy { it.id },
            activeProfileId = data.activeProfileId,
            defaultCurrencyCode = data.defaultCurrencyCode,
            filterState = meta.filterState,
            selectedAccount = selectedAccount,
            selectedAccountTransactions = meta.selectedTransactions,
            selectedAccountLedgerEntries = meta.selectedLedgerEntries,
            selectedAccountLedgerBalanceMinorUnits = ledgerDerivedBalance,
            selectedAccountReconciliation = reconciliationReport,
            activeSheet = meta.activeSheet,
            snackbarMessage = meta.snackbarMessage
        )
    }
        .catch { throwable ->
            _streamError.value = throwable.message ?: "Failed to load accounts."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState(isLoading = true)
        )

    // Filter Actions
    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onOwnershipFilterSelected(filter: OwnershipFilter) {
        _filterState.update { it.copy(ownershipFilter = filter) }
    }

    fun onAccountTypeFilterSelected(type: AccountType?) {
        _filterState.update { it.copy(selectedType = type) }
    }

    fun onShowArchivedToggled(show: Boolean) {
        _filterState.update { it.copy(showArchived = show) }
    }

    fun clearFilters() {
        _filterState.value = AccountFilterState()
    }

    // Sheet Navigation
    fun openAddAccountSheet() {
        val state = uiState.value
        val defaultCurrency = state.defaultCurrencyCode
        val defaultHouseholdId = state.households.firstOrNull()?.id
        _accountFormState.value = AccountFormState(
            editingAccountId = null,
            name = "",
            type = AccountType.BANK,
            currencyCode = defaultCurrency,
            ownershipType = OwnershipType.PERSONAL,
            selectedHouseholdId = defaultHouseholdId,
            openingBalanceInput = "0",
            openingBalanceDateEpochMs = System.currentTimeMillis(),
            description = "",
            iconIdentifier = AccountType.BANK.name,
            colorIdentifier = "BLUE"
        )
        _activeSheet.value = ActiveAccountSheet.ADD_OR_EDIT
    }

    fun openEditAccountSheet(account: Account) {
        val formattedBalance = Money(account.initialBalanceMinorUnits, account.currency)
            .toBigDecimal()
            .toPlainString()
        _accountFormState.value = AccountFormState(
            editingAccountId = account.id,
            name = account.name,
            type = account.type,
            currencyCode = account.currency.code,
            ownershipType = account.ownershipType,
            selectedHouseholdId = account.householdId,
            openingBalanceInput = formattedBalance,
            openingBalanceDateEpochMs = account.openingBalanceDateEpochMs,
            description = account.description ?: "",
            iconIdentifier = account.iconIdentifier,
            colorIdentifier = account.colorIdentifier
        )
        _activeSheet.value = ActiveAccountSheet.ADD_OR_EDIT
    }

    fun openAccountDetails(accountId: String) {
        _selectedAccountId.value = accountId
        _activeSheet.value = ActiveAccountSheet.DETAILS
    }

    fun openAdjustBalanceSheet(account: Account) {
        val currentFormatted = Money(account.currentBalanceMinorUnits, account.currency)
            .toBigDecimal()
            .toPlainString()
        _balanceAdjustmentState.value = BalanceAdjustmentFormState(
            accountId = account.id,
            accountName = account.name,
            currency = account.currency,
            currentBalanceMinorUnits = account.currentBalanceMinorUnits,
            targetBalanceInput = currentFormatted,
            reason = ""
        )
        _activeSheet.value = ActiveAccountSheet.ADJUST_BALANCE
    }

    fun dismissSheet() {
        _activeSheet.value = ActiveAccountSheet.NONE
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    // Form Mutators
    fun onAccountNameChanged(name: String) {
        _accountFormState.update { it.copy(name = name, validationError = null) }
    }

    fun onAccountTypeChanged(type: AccountType) {
        _accountFormState.update {
            it.copy(
                type = type,
                iconIdentifier = it.iconIdentifier ?: type.name,
                validationError = null
            )
        }
    }

    fun onAccountCurrencyChanged(currencyCode: String) {
        _accountFormState.update { it.copy(currencyCode = currencyCode, validationError = null) }
    }

    fun onAccountOwnershipChanged(ownershipType: OwnershipType) {
        val households = uiState.value.households
        _accountFormState.update { current ->
            val resolvedHouseholdId = if (ownershipType == OwnershipType.HOUSEHOLD) {
                current.selectedHouseholdId ?: households.firstOrNull()?.id
            } else {
                null
            }
            current.copy(
                ownershipType = ownershipType,
                selectedHouseholdId = resolvedHouseholdId,
                validationError = null
            )
        }
    }

    fun onAccountHouseholdSelected(householdId: String) {
        _accountFormState.update { it.copy(selectedHouseholdId = householdId, validationError = null) }
    }

    fun onOpeningBalanceChanged(input: String) {
        _accountFormState.update { it.copy(openingBalanceInput = input, validationError = null) }
    }

    fun onOpeningBalanceDateChanged(epochMs: Long) {
        _accountFormState.update { it.copy(openingBalanceDateEpochMs = epochMs, validationError = null) }
    }

    fun onAccountDescriptionChanged(description: String) {
        _accountFormState.update { it.copy(description = description, validationError = null) }
    }

    fun onAccountColorSelected(colorIdentifier: String) {
        _accountFormState.update { it.copy(colorIdentifier = colorIdentifier, validationError = null) }
    }

    fun onAccountIconSelected(iconIdentifier: String) {
        _accountFormState.update { it.copy(iconIdentifier = iconIdentifier, validationError = null) }
    }

    fun submitAccountForm() {
        val form = _accountFormState.value
        if (form.isSubmitting) return

        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            _accountFormState.update { it.copy(validationError = "Account name is required.") }
            return
        }

        if (form.isEditing) {
            val accountId = form.editingAccountId ?: return
            _accountFormState.update { it.copy(isSubmitting = true, validationError = null) }
            viewModelScope.launch {
                val result = accountRepository.updateAccountDetails(
                    accountId = accountId,
                    name = trimmedName,
                    type = form.type,
                    description = form.description.trim().ifEmpty { null },
                    iconIdentifier = form.iconIdentifier,
                    colorIdentifier = form.colorIdentifier
                )
                result.onSuccess {
                    _accountFormState.update { it.copy(isSubmitting = false) }
                    _activeSheet.value = ActiveAccountSheet.DETAILS
                    _selectedAccountId.value = accountId
                    _snackbarMessage.value = "Account updated."
                }.onFailure { error ->
                    _accountFormState.update {
                        it.copy(
                            isSubmitting = false,
                            validationError = error.message ?: "Failed to update account."
                        )
                    }
                }
            }
            return
        }

        // Creating a new account
        val currency = Currency.SUPPORTED_CURRENCIES.find { it.code == form.currencyCode }
        if (currency == null) {
            _accountFormState.update { it.copy(validationError = "Select a valid currency.") }
            return
        }

        if (form.ownershipType == OwnershipType.HOUSEHOLD && form.selectedHouseholdId == null) {
            _accountFormState.update { it.copy(validationError = "Select a household for this account.") }
            return
        }

        val allowNegative = form.type == AccountType.CREDIT_CARD
        val balanceResult = MoneyInputParser.parseBalanceInput(
            input = form.openingBalanceInput,
            currency = currency,
            allowNegative = allowNegative
        )
        val openingMoney = when (balanceResult) {
            is MoneyInputParser.ParseResult.Success -> balanceResult.value
            is MoneyInputParser.ParseResult.Error -> {
                _accountFormState.update { it.copy(validationError = balanceResult.message) }
                return
            }
        }

        val activeProfileId = uiState.value.activeProfileId
        val now = System.currentTimeMillis()
        val newAccount = Account(
            id = UUID.randomUUID().toString(),
            profileId = activeProfileId,
            householdId = if (form.ownershipType == OwnershipType.HOUSEHOLD) form.selectedHouseholdId else null,
            ownershipType = form.ownershipType,
            name = trimmedName,
            type = form.type,
            currency = currency,
            initialBalanceMinorUnits = openingMoney.minorUnits,
            currentBalanceMinorUnits = openingMoney.minorUnits,
            isArchived = false,
            createdAtEpochMs = now,
            openingBalanceDateEpochMs = form.openingBalanceDateEpochMs,
            description = form.description.trim().ifEmpty { null },
            iconIdentifier = form.iconIdentifier,
            colorIdentifier = form.colorIdentifier
        )

        _accountFormState.update { it.copy(isSubmitting = true, validationError = null) }
        viewModelScope.launch {
            val result = accountRepository.createAccount(newAccount)
            result.onSuccess {
                _accountFormState.update { it.copy(isSubmitting = false) }
                _activeSheet.value = ActiveAccountSheet.NONE
                _snackbarMessage.value = "Account '${newAccount.name}' created."
            }.onFailure { error ->
                _accountFormState.update {
                    it.copy(
                        isSubmitting = false,
                        validationError = error.message ?: "Failed to create account."
                    )
                }
            }
        }
    }

    // Auditable Balance Adjustment
    fun onTargetBalanceChanged(input: String) {
        _balanceAdjustmentState.update { it.copy(targetBalanceInput = input, validationError = null) }
    }

    fun onAdjustmentReasonChanged(reason: String) {
        _balanceAdjustmentState.update { it.copy(reason = reason, validationError = null) }
    }

    fun submitBalanceAdjustment() {
        val state = _balanceAdjustmentState.value
        if (state.isSubmitting) return

        val parsed = MoneyInputParser.parseBalanceInput(
            input = state.targetBalanceInput,
            currency = state.currency,
            allowNegative = true
        )
        val targetMoney = when (parsed) {
            is MoneyInputParser.ParseResult.Success -> parsed.value
            is MoneyInputParser.ParseResult.Error -> {
                _balanceAdjustmentState.update { it.copy(validationError = parsed.message) }
                return
            }
        }

        _balanceAdjustmentState.update { it.copy(isSubmitting = true, validationError = null) }
        viewModelScope.launch {
            val result = accountRepository.adjustBalance(
                accountId = state.accountId,
                targetBalanceMinorUnits = targetMoney.minorUnits,
                reason = state.reason.trim().ifEmpty { null }
            )
            result.onSuccess {
                _balanceAdjustmentState.update { it.copy(isSubmitting = false) }
                _activeSheet.value = ActiveAccountSheet.DETAILS
                _snackbarMessage.value = "Balance adjustment recorded in ledger."
            }.onFailure { error ->
                _balanceAdjustmentState.update {
                    it.copy(
                        isSubmitting = false,
                        validationError = error.message ?: "Failed to adjust balance."
                    )
                }
            }
        }
    }

    // Archive / Unarchive
    fun archiveAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.archiveAccount(accountId)
                .onSuccess {
                    _activeSheet.value = ActiveAccountSheet.NONE
                    _snackbarMessage.value = "Account archived. Historical transactions remain preserved."
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "Failed to archive account."
                }
        }
    }

    fun unarchiveAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.unarchiveAccount(accountId)
                .onSuccess {
                    _snackbarMessage.value = "Account restored."
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "Failed to restore account."
                }
        }
    }

    private fun applyFilters(
        accounts: List<Account>,
        filter: AccountFilterState
    ): List<Account> {
        val query = filter.searchQuery.trim().lowercase()
        return accounts.filter { account ->
            val archivedMatch = filter.showArchived || !account.isArchived
            val ownershipMatch = when (filter.ownershipFilter) {
                OwnershipFilter.ALL -> true
                OwnershipFilter.PERSONAL -> account.ownershipType == OwnershipType.PERSONAL
                OwnershipFilter.HOUSEHOLD -> account.ownershipType == OwnershipType.HOUSEHOLD
            }
            val typeMatch = filter.selectedType == null || account.type == filter.selectedType
            val searchMatch = query.isEmpty() ||
                account.name.lowercase().contains(query) ||
                account.currency.code.lowercase().contains(query) ||
                account.type.displayName.lowercase().contains(query) ||
                (account.description?.lowercase()?.contains(query) == true)

            archivedMatch && ownershipMatch && typeMatch && searchMatch
        }
    }
}
