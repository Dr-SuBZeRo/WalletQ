package com.walletq.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.repository.BudgetRepository
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.datastore.PreferencesRepository
import com.walletq.core.model.Budget
import com.walletq.core.model.BudgetCalculationEngine
import com.walletq.core.model.BudgetCategoryAllocation
import com.walletq.core.model.BudgetPeriod
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionType
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val householdDao: HouseholdDao,
    private val profileDao: ProfileDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(BudgetFilterState())
    private val _isFormSheetOpen = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _streamError = MutableStateFlow<String?>(null)

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private val activeProfileIdFlow: Flow<String> =
        profileDao.getDefaultProfileFlow().map { it?.id ?: "default-personal" }

    private val accessibleBudgetsFlow: Flow<List<Budget>> =
        activeProfileIdFlow.flatMapLatest { profileId ->
            budgetRepository.observeAccessibleBudgets(profileId)
        }

    private data class CoreBudgetData(
        val budgets: List<Budget>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val households: List<Household>,
        val activeProfileId: String,
        val defaultCurrencyCode: String
    )

    private val coreDataFlow: Flow<CoreBudgetData> = combine(
        accessibleBudgetsFlow,
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        householdDao.getAllHouseholdsFlow().map { list -> list.map { it.toDomain() } },
        activeProfileIdFlow,
        preferencesRepository.userPreferencesFlow.map { it.defaultCurrencyCode }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoreBudgetData(
            budgets = values[0] as List<Budget>,
            transactions = values[1] as List<Transaction>,
            categories = values[2] as List<Category>,
            households = values[3] as List<Household>,
            activeProfileId = values[4] as String,
            defaultCurrencyCode = values[5] as String
        )
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        coreDataFlow,
        _filterState,
        _isFormSheetOpen,
        _snackbarMessage,
        _streamError
    ) { data, filter, isSheetOpen, snackbar, streamError ->
        val filteredBudgets = data.budgets.filter { budget ->
            val archivedMatch = filter.showArchived || !budget.isArchived
            val ownershipMatch = when (filter.ownershipFilter) {
                OwnershipFilter.ALL -> true
                OwnershipFilter.PERSONAL -> budget.ownershipType == OwnershipType.PERSONAL
                OwnershipFilter.HOUSEHOLD -> budget.ownershipType == OwnershipType.HOUSEHOLD
            }
            archivedMatch && ownershipMatch
        }

        val summaries = filteredBudgets.map { budget ->
            BudgetCalculationEngine.calculateBudgetSpending(budget, data.transactions)
        }

        val expenseCategories = data.categories.filter {
            it.transactionType == TransactionType.EXPENSE && !it.isArchived
        }

        BudgetsUiState(
            isLoading = false,
            errorMessage = streamError,
            budgetSummaries = summaries,
            expenseCategories = expenseCategories,
            categoriesById = data.categories.associateBy { it.id },
            households = data.households,
            householdsById = data.households.associateBy { it.id },
            activeProfileId = data.activeProfileId,
            defaultCurrencyCode = data.defaultCurrencyCode,
            filterState = filter,
            isFormSheetOpen = isSheetOpen,
            snackbarMessage = snackbar
        )
    }
        .catch { throwable ->
            _streamError.value = throwable.message ?: "Failed to load budgets."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetsUiState(isLoading = true)
        )

    fun onOwnershipFilterSelected(filter: OwnershipFilter) {
        _filterState.update { it.copy(ownershipFilter = filter) }
    }

    fun onShowArchivedToggled(show: Boolean) {
        _filterState.update { it.copy(showArchived = show) }
    }

    fun openCreateBudgetSheet() {
        val state = uiState.value
        _formState.value = BudgetFormState(
            editingBudgetId = null,
            name = "",
            yearMonth = YearMonth.now(),
            currencyCode = state.defaultCurrencyCode,
            allocatedAmountInput = "",
            ownershipType = OwnershipType.PERSONAL,
            selectedHouseholdId = state.households.firstOrNull()?.id,
            categoryAllocations = emptyList()
        )
        _isFormSheetOpen.value = true
    }

    fun openEditBudgetSheet(budget: Budget) {
        val ym = Instant.ofEpochMilli(budget.startDateEpochMs)
            .atZone(ZoneId.systemDefault())
            .let { YearMonth.of(it.year, it.month) }
        val amountStr = Money(budget.allocatedAmountMinorUnits, budget.currency)
            .toBigDecimal()
            .toPlainString()
        val draftAllocations = budget.categoryAllocations.map { alloc ->
            CategoryAllocationDraft(
                id = alloc.id,
                categoryId = alloc.categoryId,
                amountInput = Money(alloc.allocatedAmountMinorUnits, budget.currency)
                    .toBigDecimal()
                    .toPlainString()
            )
        }
        _formState.value = BudgetFormState(
            editingBudgetId = budget.id,
            name = budget.name,
            yearMonth = ym,
            currencyCode = budget.currency.code,
            allocatedAmountInput = amountStr,
            ownershipType = budget.ownershipType,
            selectedHouseholdId = budget.householdId,
            categoryAllocations = draftAllocations
        )
        _isFormSheetOpen.value = true
    }

    fun dismissFormSheet() {
        _isFormSheetOpen.value = false
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun onNameChanged(name: String) {
        _formState.update { it.copy(name = name, validationError = null) }
    }

    fun onYearMonthChanged(yearMonth: YearMonth) {
        _formState.update { it.copy(yearMonth = yearMonth, validationError = null) }
    }

    fun onCurrencyChanged(currencyCode: String) {
        _formState.update { it.copy(currencyCode = currencyCode, validationError = null) }
    }

    fun onAllocatedAmountChanged(input: String) {
        _formState.update { it.copy(allocatedAmountInput = input, validationError = null) }
    }

    fun onOwnershipChanged(ownershipType: OwnershipType) {
        val households = uiState.value.households
        _formState.update { current ->
            val resolvedHouseholdId = if (ownershipType == OwnershipType.HOUSEHOLD) {
                current.selectedHouseholdId ?: households.firstOrNull()?.id
            } else null
            current.copy(
                ownershipType = ownershipType,
                selectedHouseholdId = resolvedHouseholdId,
                validationError = null
            )
        }
    }

    fun onHouseholdSelected(householdId: String) {
        _formState.update { it.copy(selectedHouseholdId = householdId, validationError = null) }
    }

    fun addCategoryAllocation(categoryId: String) {
        _formState.update { current ->
            if (current.categoryAllocations.any { it.categoryId == categoryId }) {
                return@update current.copy(validationError = "Category is already allocated in this budget.")
            }
            current.copy(
                categoryAllocations = current.categoryAllocations + CategoryAllocationDraft(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    amountInput = ""
                ),
                validationError = null
            )
        }
    }

    fun updateCategoryAllocationAmount(allocationId: String, amountInput: String) {
        _formState.update { current ->
            current.copy(
                categoryAllocations = current.categoryAllocations.map {
                    if (it.id == allocationId) it.copy(amountInput = amountInput) else it
                },
                validationError = null
            )
        }
    }

    fun removeCategoryAllocation(allocationId: String) {
        _formState.update { current ->
            current.copy(
                categoryAllocations = current.categoryAllocations.filterNot { it.id == allocationId },
                validationError = null
            )
        }
    }

    fun submitBudgetForm() {
        val form = _formState.value
        if (form.isSubmitting) return

        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            _formState.update { it.copy(validationError = "Budget name is required.") }
            return
        }

        val currency = Currency.SUPPORTED_CURRENCIES.find { it.code == form.currencyCode }
        if (currency == null) {
            _formState.update { it.copy(validationError = "Select a valid currency.") }
            return
        }

        val amountResult = MoneyInputParser.parseAmount(form.allocatedAmountInput, currency)
        val totalMoney = when (amountResult) {
            is MoneyInputParser.ParseResult.Success -> amountResult.value
            is MoneyInputParser.ParseResult.Error -> {
                _formState.update { it.copy(validationError = amountResult.message) }
                return
            }
        }

        if (form.ownershipType == OwnershipType.HOUSEHOLD && form.selectedHouseholdId == null) {
            _formState.update { it.copy(validationError = "Select a household for this budget.") }
            return
        }

        val budgetId = form.editingBudgetId ?: UUID.randomUUID().toString()
        val parsedAllocations = mutableListOf<BudgetCategoryAllocation>()
        val seenCategories = mutableSetOf<String>()

        for (draft in form.categoryAllocations) {
            if (!seenCategories.add(draft.categoryId)) {
                _formState.update { it.copy(validationError = "Duplicate category allocation is not allowed.") }
                return
            }
            val allocResult = MoneyInputParser.parseAmount(draft.amountInput, currency)
            val allocMoney = when (allocResult) {
                is MoneyInputParser.ParseResult.Success -> allocResult.value
                is MoneyInputParser.ParseResult.Error -> {
                    val catName = uiState.value.categoriesById[draft.categoryId]?.name ?: "Category"
                    _formState.update { it.copy(validationError = "$catName: ${allocResult.message}") }
                    return
                }
            }
            parsedAllocations.add(
                BudgetCategoryAllocation(
                    id = draft.id,
                    budgetId = budgetId,
                    categoryId = draft.categoryId,
                    allocatedAmountMinorUnits = allocMoney.minorUnits
                )
            )
        }

        val (startMs, endMs) = BudgetCalculationEngine.monthBoundaries(form.yearMonth)
        val profileId = uiState.value.activeProfileId
        val now = System.currentTimeMillis()

        val budget = Budget(
            id = budgetId,
            name = trimmedName,
            period = BudgetPeriod.MONTHLY,
            startDateEpochMs = startMs,
            endDateEpochMs = endMs,
            currency = currency,
            allocatedAmountMinorUnits = totalMoney.minorUnits,
            ownershipType = form.ownershipType,
            profileId = if (form.ownershipType == OwnershipType.PERSONAL) profileId else null,
            householdId = if (form.ownershipType == OwnershipType.HOUSEHOLD) form.selectedHouseholdId else null,
            isArchived = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            categoryAllocations = parsedAllocations
        )

        _formState.update { it.copy(isSubmitting = true, validationError = null) }
        viewModelScope.launch {
            val result = if (form.isEditing) {
                budgetRepository.updateBudget(budget, profileId)
            } else {
                budgetRepository.createBudget(budget, profileId)
            }
            result.onSuccess {
                _formState.update { it.copy(isSubmitting = false) }
                _isFormSheetOpen.value = false
                _snackbarMessage.value = if (form.isEditing) "Budget updated." else "Budget created."
            }.onFailure { error ->
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        validationError = error.message ?: "Failed to save budget."
                    )
                }
            }
        }
    }

    fun archiveBudget(budgetId: String) {
        val profileId = uiState.value.activeProfileId
        viewModelScope.launch {
            budgetRepository.archiveBudget(budgetId, profileId)
                .onSuccess { _snackbarMessage.value = "Budget archived." }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to archive budget." }
        }
    }

    fun unarchiveBudget(budgetId: String) {
        val profileId = uiState.value.activeProfileId
        viewModelScope.launch {
            budgetRepository.unarchiveBudget(budgetId, profileId)
                .onSuccess { _snackbarMessage.value = "Budget restored." }
                .onFailure { _snackbarMessage.value = it.message ?: "Failed to restore budget." }
        }
    }
}
