package com.walletq.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.ExchangeRateDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.model.Account
import com.walletq.core.model.Category
import com.walletq.core.model.Household
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.core.model.TransferDetails
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountDao: AccountDao,
    private val profileDao: ProfileDao,
    private val householdDao: HouseholdDao,
    private val exchangeRateDao: ExchangeRateDao
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionFilterState())
    private val _selectedTransactionId = MutableStateFlow<String?>(null)
    private val _activeSheet = MutableStateFlow(ActiveActivitySheet.NONE)
    private val _isReversing = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _streamError = MutableStateFlow<String?>(null)

    private val _transactionFormState = MutableStateFlow(TransactionFormState())
    val transactionFormState: StateFlow<TransactionFormState> = _transactionFormState.asStateFlow()

    private val _transferFormState = MutableStateFlow(TransferFormState())
    val transferFormState: StateFlow<TransferFormState> = _transferFormState.asStateFlow()

    private val selectedLedgerEntriesFlow: Flow<List<LedgerEntry>> =
        _selectedTransactionId.flatMapLatest { txId ->
            if (txId == null) flowOf(emptyList())
            else transactionRepository.getLedgerEntriesForTransaction(txId)
        }

    private data class CoreDataBundle(
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val households: List<Household>,
        val activeProfileId: String
    )

    private val coreDataFlow: Flow<CoreDataBundle> = combine(
        transactionRepository.getTransactions(),
        accountDao.observeAllAccountsFlow().map { list -> list.map { it.toDomain() } },
        categoryRepository.getActiveCategories(),
        householdDao.getAllHouseholdsFlow().map { list -> list.map { it.toDomain() } },
        profileDao.getDefaultProfileFlow().map { it?.id ?: "default-personal" }
    ) { transactions, accounts, categories, households, profileId ->
        CoreDataBundle(
            transactions = transactions,
            accounts = accounts,
            categories = categories,
            households = households,
            activeProfileId = profileId
        )
    }

    private data class UiMetaBundle(
        val filterState: TransactionFilterState,
        val selectedTransactionId: String?,
        val selectedLedgerEntries: List<LedgerEntry>,
        val activeSheet: ActiveActivitySheet,
        val isReversing: Boolean,
        val snackbarMessage: String?,
        val streamError: String?
    )

    private val uiMetaFlow: Flow<UiMetaBundle> = combine(
        _filterState,
        _selectedTransactionId,
        selectedLedgerEntriesFlow,
        _activeSheet,
        _isReversing,
        _snackbarMessage,
        _streamError
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        UiMetaBundle(
            filterState = values[0] as TransactionFilterState,
            selectedTransactionId = values[1] as String?,
            selectedLedgerEntries = values[2] as List<LedgerEntry>,
            activeSheet = values[3] as ActiveActivitySheet,
            isReversing = values[4] as Boolean,
            snackbarMessage = values[5] as String?,
            streamError = values[6] as String?
        )
    }

    val uiState: StateFlow<ActivityUiState> = combine(coreDataFlow, uiMetaFlow) { data, meta ->
        val activeAccounts = data.accounts.filter { !it.isArchived }
        val accountsById = data.accounts.associateBy { it.id }
        val categoriesById = data.categories.associateBy { it.id }
        val householdsById = data.households.associateBy { it.id }

        val filtered = applyFilters(
            transactions = data.transactions,
            filter = meta.filterState,
            accountsById = accountsById,
            categoriesById = categoriesById
        )

        val grouped = groupTransactionsByDate(filtered)
        val selectedTx = meta.selectedTransactionId?.let { id ->
            data.transactions.find { it.id == id }
        }

        ActivityUiState(
            isLoading = false,
            errorMessage = meta.streamError,
            transactions = filtered,
            groupedTransactions = grouped,
            accounts = activeAccounts,
            accountsById = accountsById,
            categories = data.categories,
            categoriesById = categoriesById,
            householdsById = householdsById,
            activeProfileId = data.activeProfileId,
            filterState = meta.filterState,
            selectedTransaction = selectedTx,
            selectedTransactionLedgerEntries = meta.selectedLedgerEntries,
            isReversing = meta.isReversing,
            activeSheet = meta.activeSheet,
            snackbarMessage = meta.snackbarMessage
        )
    }
        .catch { e ->
            emit(
                ActivityUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load transactions."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActivityUiState(isLoading = true)
        )

    // --- Filter Actions ---

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onOwnershipFilterChanged(filter: OwnershipFilter) {
        _filterState.update { it.copy(ownershipFilter = filter) }
    }

    fun onTypeFilterChanged(type: TransactionType?) {
        _filterState.update { it.copy(selectedType = type) }
    }

    fun onAccountFilterChanged(accountId: String?) {
        _filterState.update { it.copy(selectedAccountId = accountId) }
    }

    fun onDateRangeFilterChanged(dateRangeFilter: DateRangeFilter) {
        _filterState.update { it.copy(dateRangeFilter = dateRangeFilter) }
    }

    fun clearFilters() {
        _filterState.value = TransactionFilterState()
    }

    // --- Sheet & Navigation Actions ---

    fun openAddTransactionSheet(type: TransactionType = TransactionType.EXPENSE) {
        val currentAccounts = uiState.value.accounts
        val defaultAccount = currentAccounts.firstOrNull {
            it.ownershipType == OwnershipType.PERSONAL
        } ?: currentAccounts.firstOrNull()

        val ownership = defaultAccount?.ownershipType ?: OwnershipType.PERSONAL
        val defaultCategory = findDefaultCategoryFor(type, defaultAccount, uiState.value.categories)

        _transactionFormState.value = TransactionFormState(
            transactionType = type,
            amountInput = "",
            selectedAccountId = defaultAccount?.id,
            selectedCategoryId = defaultCategory?.id,
            dateEpochMs = System.currentTimeMillis(),
            description = "",
            notes = "",
            ownershipContext = ownership,
            validationError = null,
            isSubmitting = false
        )
        _activeSheet.value = ActiveActivitySheet.ADD_TRANSACTION
    }

    fun openTransferSheet() {
        val currentAccounts = uiState.value.accounts
        val source = currentAccounts.firstOrNull()
        val dest = currentAccounts.firstOrNull { it.id != source?.id }
        val isCrossCurrency = source != null && dest != null && source.currency.code != dest.currency.code

        _transferFormState.value = TransferFormState(
            sourceAccountId = source?.id,
            destinationAccountId = dest?.id,
            sourceAmountInput = "",
            exchangeRateInput = "",
            exchangeRateReferenceId = null,
            dateEpochMs = System.currentTimeMillis(),
            notes = "",
            isCrossCurrency = isCrossCurrency,
            destinationPreview = null,
            validationError = null,
            isSubmitting = false
        )
        _activeSheet.value = ActiveActivitySheet.TRANSFER

        if (source != null && dest != null && isCrossCurrency) {
            prefillLatestExchangeRate(source.currency.code, dest.currency.code)
        }
    }

    fun openTransactionDetails(transactionId: String) {
        _selectedTransactionId.value = transactionId
        _activeSheet.value = ActiveActivitySheet.DETAILS
    }

    fun dismissSheet() {
        _activeSheet.value = ActiveActivitySheet.NONE
        _selectedTransactionId.value = null
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    // --- Income / Expense Form Actions ---

    fun onFormTypeChanged(type: TransactionType) {
        if (type == TransactionType.TRANSFER) return
        val account = _transactionFormState.value.selectedAccountId?.let { uiState.value.accountsById[it] }
        val currentCategory = _transactionFormState.value.selectedCategoryId?.let { uiState.value.categoriesById[it] }
        val validCategory = if (currentCategory?.transactionType == type) currentCategory else {
            findDefaultCategoryFor(type, account, uiState.value.categories)
        }

        _transactionFormState.update {
            it.copy(
                transactionType = type,
                selectedCategoryId = validCategory?.id,
                validationError = null
            )
        }
    }

    fun onFormAmountChanged(input: String) {
        _transactionFormState.update { it.copy(amountInput = input, validationError = null) }
    }

    fun onFormOwnershipContextChanged(ownership: OwnershipType) {
        val matchingAccount = uiState.value.accounts.firstOrNull { it.ownershipType == ownership }
        val currentAccount = _transactionFormState.value.selectedAccountId?.let { uiState.value.accountsById[it] }
        val nextAccount = if (currentAccount?.ownershipType == ownership) currentAccount else matchingAccount

        val nextCategory = findDefaultCategoryFor(
            _transactionFormState.value.transactionType,
            nextAccount,
            uiState.value.categories
        )

        _transactionFormState.update {
            it.copy(
                ownershipContext = ownership,
                selectedAccountId = nextAccount?.id,
                selectedCategoryId = nextCategory?.id,
                validationError = null
            )
        }
    }

    fun onFormAccountSelected(accountId: String) {
        val account = uiState.value.accountsById[accountId] ?: return
        val currentCategory = _transactionFormState.value.selectedCategoryId?.let { uiState.value.categoriesById[it] }
        val validCategory = if (currentCategory != null && isCategoryValidForAccount(currentCategory, account)) {
            currentCategory
        } else {
            findDefaultCategoryFor(_transactionFormState.value.transactionType, account, uiState.value.categories)
        }

        _transactionFormState.update {
            it.copy(
                selectedAccountId = accountId,
                ownershipContext = account.ownershipType,
                selectedCategoryId = validCategory?.id,
                validationError = null
            )
        }
    }

    fun onFormCategorySelected(categoryId: String) {
        _transactionFormState.update { it.copy(selectedCategoryId = categoryId, validationError = null) }
    }

    fun onFormDateSelected(dateEpochMs: Long) {
        _transactionFormState.update { it.copy(dateEpochMs = dateEpochMs, validationError = null) }
    }

    fun onFormDescriptionChanged(description: String) {
        _transactionFormState.update { it.copy(description = description, validationError = null) }
    }

    fun onFormNotesChanged(notes: String) {
        _transactionFormState.update { it.copy(notes = notes, validationError = null) }
    }

    fun submitIncomeOrExpense() {
        val form = _transactionFormState.value
        if (form.isSubmitting) return // Prevent duplicate submissions

        val accountId = form.selectedAccountId
        if (accountId == null) {
            _transactionFormState.update { it.copy(validationError = "Please select an account.") }
            return
        }

        val account = uiState.value.accountsById[accountId]
        if (account == null) {
            _transactionFormState.update { it.copy(validationError = "Selected account is no longer available.") }
            return
        }

        val amountResult = MoneyInputParser.parseAmount(form.amountInput, account.currency)
        if (amountResult is MoneyInputParser.ParseResult.Error) {
            _transactionFormState.update { it.copy(validationError = amountResult.message) }
            return
        }
        val money = (amountResult as MoneyInputParser.ParseResult.Success).value

        val description = form.description.trim()
        if (description.isEmpty()) {
            _transactionFormState.update { it.copy(validationError = "Please enter a description.") }
            return
        }

        val categoryId = form.selectedCategoryId
        if (categoryId != null) {
            val category = uiState.value.categoriesById[categoryId]
            if (category == null || !isCategoryValidForAccount(category, account) || category.transactionType != form.transactionType) {
                _transactionFormState.update { it.copy(validationError = "Selected category is invalid for this account or transaction type.") }
                return
            }
        }

        val now = System.currentTimeMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            type = form.transactionType,
            amount = money,
            accountId = account.id,
            categoryId = categoryId,
            transactionDateEpochMs = form.dateEpochMs,
            description = description,
            notes = form.notes.trim().ifEmpty { null },
            ownershipType = account.ownershipType,
            ownerProfileId = account.profileId,
            householdId = if (account.ownershipType == OwnershipType.HOUSEHOLD) account.householdId else null,
            transferDetails = null,
            status = TransactionStatus.POSTED,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        _transactionFormState.update { it.copy(isSubmitting = true, validationError = null) }

        viewModelScope.launch {
            val result = transactionRepository.recordTransaction(transaction)
            result.fold(
                onSuccess = {
                    _transactionFormState.update { it.copy(isSubmitting = false) }
                    _activeSheet.value = ActiveActivitySheet.NONE
                    val label = if (transaction.type == TransactionType.INCOME) "Income" else "Expense"
                    _snackbarMessage.value = "$label recorded: ${money.format()}"
                },
                onFailure = { error ->
                    _transactionFormState.update {
                        it.copy(
                            isSubmitting = false,
                            validationError = error.message ?: "Failed to save transaction."
                        )
                    }
                }
            )
        }
    }

    // --- Transfer Form Actions ---

    fun onTransferSourceAccountSelected(accountId: String) {
        val state = _transferFormState.value
        val source = uiState.value.accountsById[accountId] ?: return
        val dest = state.destinationAccountId?.let { uiState.value.accountsById[it] }
        val isCross = dest != null && source.currency.code != dest.currency.code

        _transferFormState.update {
            it.copy(
                sourceAccountId = accountId,
                isCrossCurrency = isCross,
                exchangeRateInput = if (!isCross) "" else it.exchangeRateInput,
                exchangeRateReferenceId = if (!isCross) null else it.exchangeRateReferenceId,
                validationError = null
            )
        }
        recomputeTransferPreview()
        if (dest != null && isCross) {
            prefillLatestExchangeRate(source.currency.code, dest.currency.code)
        }
    }

    fun onTransferDestinationAccountSelected(accountId: String) {
        val state = _transferFormState.value
        val dest = uiState.value.accountsById[accountId] ?: return
        val source = state.sourceAccountId?.let { uiState.value.accountsById[it] }
        val isCross = source != null && source.currency.code != dest.currency.code

        _transferFormState.update {
            it.copy(
                destinationAccountId = accountId,
                isCrossCurrency = isCross,
                exchangeRateInput = if (!isCross) "" else it.exchangeRateInput,
                exchangeRateReferenceId = if (!isCross) null else it.exchangeRateReferenceId,
                validationError = null
            )
        }
        recomputeTransferPreview()
        if (source != null && isCross) {
            prefillLatestExchangeRate(source.currency.code, dest.currency.code)
        }
    }

    fun onTransferAmountChanged(input: String) {
        _transferFormState.update { it.copy(sourceAmountInput = input, validationError = null) }
        recomputeTransferPreview()
    }

    fun onTransferExchangeRateChanged(input: String) {
        _transferFormState.update {
            it.copy(
                exchangeRateInput = input,
                exchangeRateReferenceId = null,
                validationError = null
            )
        }
        recomputeTransferPreview()
    }

    fun onTransferDateSelected(dateEpochMs: Long) {
        _transferFormState.update { it.copy(dateEpochMs = dateEpochMs, validationError = null) }
    }

    fun onTransferNotesChanged(notes: String) {
        _transferFormState.update { it.copy(notes = notes, validationError = null) }
    }

    private fun prefillLatestExchangeRate(baseCode: String, targetCode: String) {
        viewModelScope.launch {
            val latest = exchangeRateDao.getLatestRate(baseCode, targetCode)
            if (latest != null) {
                val current = _transferFormState.value
                val source = current.sourceAccountId?.let { uiState.value.accountsById[it] }
                val dest = current.destinationAccountId?.let { uiState.value.accountsById[it] }
                if (source?.currency?.code == baseCode && dest?.currency?.code == targetCode && current.exchangeRateInput.isBlank()) {
                    _transferFormState.update {
                        it.copy(
                            exchangeRateInput = latest.rate,
                            exchangeRateReferenceId = latest.id
                        )
                    }
                    recomputeTransferPreview()
                }
            }
        }
    }

    private fun recomputeTransferPreview() {
        val state = _transferFormState.value
        val source = state.sourceAccountId?.let { uiState.value.accountsById[it] }
        val dest = state.destinationAccountId?.let { uiState.value.accountsById[it] }

        val preview = if (source != null && dest != null) {
            MoneyInputParser.computeDestinationPreview(
                sourceAmountInput = state.sourceAmountInput,
                sourceCurrency = source.currency,
                destinationCurrency = dest.currency,
                exchangeRateInput = state.exchangeRateInput
            )
        } else null

        _transferFormState.update { it.copy(destinationPreview = preview) }
    }

    fun submitTransfer() {
        val form = _transferFormState.value
        if (form.isSubmitting) return // Prevent duplicate submissions

        val sourceId = form.sourceAccountId
        val destId = form.destinationAccountId
        if (sourceId == null || destId == null) {
            _transferFormState.update { it.copy(validationError = "Please select both source and destination accounts.") }
            return
        }

        if (sourceId == destId) {
            _transferFormState.update { it.copy(validationError = "Source and destination accounts must be different.") }
            return
        }

        val sourceAccount = uiState.value.accountsById[sourceId]
        val destAccount = uiState.value.accountsById[destId]
        if (sourceAccount == null || destAccount == null) {
            _transferFormState.update { it.copy(validationError = "Selected accounts are no longer available.") }
            return
        }

        val amountResult = MoneyInputParser.parseAmount(form.sourceAmountInput, sourceAccount.currency)
        if (amountResult is MoneyInputParser.ParseResult.Error) {
            _transferFormState.update { it.copy(validationError = amountResult.message) }
            return
        }
        val sourceMoney = (amountResult as MoneyInputParser.ParseResult.Success).value

        val isCrossCurrency = sourceAccount.currency.code != destAccount.currency.code
        val appliedRate: BigDecimal?
        val destinationMoney = if (!isCrossCurrency) {
            appliedRate = BigDecimal.ONE
            sourceMoney
        } else {
            val rateResult = MoneyInputParser.parseExchangeRate(form.exchangeRateInput)
            if (rateResult is MoneyInputParser.ParseResult.Error) {
                _transferFormState.update { it.copy(validationError = rateResult.message) }
                return
            }
            val rate = (rateResult as MoneyInputParser.ParseResult.Success).value
            appliedRate = rate
            try {
                val converted = sourceMoney.convertTo(destAccount.currency, rate)
                if (!converted.isPositive()) {
                    _transferFormState.update { it.copy(validationError = "Converted destination amount must be greater than zero.") }
                    return
                }
                converted
            } catch (_: ArithmeticException) {
                _transferFormState.update { it.copy(validationError = "Converted amount exceeds maximum supported limit.") }
                return
            }
        }

        val transferOwnership = if (sourceAccount.ownershipType == OwnershipType.HOUSEHOLD) {
            OwnershipType.HOUSEHOLD
        } else {
            OwnershipType.PERSONAL
        }

        val now = System.currentTimeMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            type = TransactionType.TRANSFER,
            amount = sourceMoney,
            accountId = sourceAccount.id,
            categoryId = null,
            transactionDateEpochMs = form.dateEpochMs,
            description = "Transfer: ${sourceAccount.name} → ${destAccount.name}",
            notes = form.notes.trim().ifEmpty { null },
            ownershipType = transferOwnership,
            ownerProfileId = sourceAccount.profileId,
            householdId = if (transferOwnership == OwnershipType.HOUSEHOLD) sourceAccount.householdId else null,
            transferDetails = TransferDetails(
                destinationAccountId = destAccount.id,
                destinationAmount = destinationMoney,
                appliedExchangeRate = appliedRate,
                exchangeRateReferenceId = if (isCrossCurrency) form.exchangeRateReferenceId else null
            ),
            status = TransactionStatus.POSTED,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        _transferFormState.update { it.copy(isSubmitting = true, validationError = null) }

        viewModelScope.launch {
            val result = transactionRepository.recordTransaction(transaction)
            result.fold(
                onSuccess = {
                    _transferFormState.update { it.copy(isSubmitting = false) }
                    _activeSheet.value = ActiveActivitySheet.NONE
                    _snackbarMessage.value = "Transfer completed: ${sourceMoney.format()}"
                },
                onFailure = { error ->
                    _transferFormState.update {
                        it.copy(
                            isSubmitting = false,
                            validationError = error.message ?: "Transfer validation failed."
                        )
                    }
                }
            )
        }
    }

    // --- Reversal Workflow ---

    fun reverseSelectedTransaction(reason: String = "User requested reversal") {
        val tx = uiState.value.selectedTransaction ?: return
        if (_isReversing.value || tx.status != TransactionStatus.POSTED) return

        _isReversing.value = true
        viewModelScope.launch {
            val result = transactionRepository.reverseTransaction(
                transactionId = tx.id,
                reason = reason.trim().ifEmpty { "User requested reversal" }
            )
            result.fold(
                onSuccess = {
                    _isReversing.value = false
                    _snackbarMessage.value = "Transaction reversed with compensating ledger entry."
                },
                onFailure = { error ->
                    _isReversing.value = false
                    _snackbarMessage.value = error.message ?: "Failed to reverse transaction."
                }
            )
        }
    }

    // --- Helpers ---

    fun isCategoryValidForAccount(category: Category, account: Account?): Boolean {
        if (category.isSystemCategory) return true
        if (account == null) return false
        return when (account.ownershipType) {
            OwnershipType.PERSONAL -> category.profileId == account.profileId && category.householdId == null
            OwnershipType.HOUSEHOLD -> category.householdId == account.householdId && category.householdId != null
        }
    }

    private fun findDefaultCategoryFor(
        type: TransactionType,
        account: Account?,
        categories: List<Category>
    ): Category? {
        return categories.firstOrNull { cat ->
            cat.transactionType == type && isCategoryValidForAccount(cat, account)
        }
    }

    private fun applyFilters(
        transactions: List<Transaction>,
        filter: TransactionFilterState,
        accountsById: Map<String, Account>,
        categoriesById: Map<String, Category>
    ): List<Transaction> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfTodayMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrowMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOf7DaysMs = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfMonthMs = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextMonthMs = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val query = filter.searchQuery.trim()

        return transactions.filter { tx ->
            val matchesOwnership = when (filter.ownershipFilter) {
                OwnershipFilter.ALL -> true
                OwnershipFilter.PERSONAL -> tx.ownershipType == OwnershipType.PERSONAL
                OwnershipFilter.HOUSEHOLD -> tx.ownershipType == OwnershipType.HOUSEHOLD
            }
            if (!matchesOwnership) return@filter false

            if (filter.selectedType != null && tx.type != filter.selectedType) {
                return@filter false
            }

            if (filter.selectedAccountId != null &&
                tx.accountId != filter.selectedAccountId &&
                tx.transferDetails?.destinationAccountId != filter.selectedAccountId
            ) {
                return@filter false
            }

            val matchesDate = when (filter.dateRangeFilter) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> tx.transactionDateEpochMs in startOfTodayMs until startOfTomorrowMs
                DateRangeFilter.LAST_7_DAYS -> tx.transactionDateEpochMs in startOf7DaysMs until startOfTomorrowMs
                DateRangeFilter.THIS_MONTH -> tx.transactionDateEpochMs in startOfMonthMs until startOfNextMonthMs
            }
            if (!matchesDate) return@filter false

            if (query.isNotEmpty()) {
                val accountName = accountsById[tx.accountId]?.name.orEmpty()
                val destName = tx.transferDetails?.destinationAccountId?.let { accountsById[it]?.name }.orEmpty()
                val catName = tx.categoryId?.let { categoriesById[it]?.name }.orEmpty()
                val matchesText = tx.description.contains(query, ignoreCase = true) ||
                    (tx.notes?.contains(query, ignoreCase = true) == true) ||
                    accountName.contains(query, ignoreCase = true) ||
                    destName.contains(query, ignoreCase = true) ||
                    catName.contains(query, ignoreCase = true)
                if (!matchesText) return@filter false
            }

            true
        }
    }

    private fun groupTransactionsByDate(transactions: List<Transaction>): List<TransactionDateGroup> {
        if (transactions.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

        return transactions
            .groupBy { tx ->
                Instant.ofEpochMilli(tx.transactionDateEpochMs).atZone(zone).toLocalDate()
            }
            .entries
            .sortedByDescending { it.key }
            .map { (date, list) ->
                val label = when (date) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> date.format(formatter)
                }
                TransactionDateGroup(
                    epochDay = date.toEpochDay(),
                    dateLabel = label,
                    transactions = list
                )
            }
    }
}
