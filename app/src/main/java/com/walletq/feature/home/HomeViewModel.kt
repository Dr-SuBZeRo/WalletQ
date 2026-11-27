package com.walletq.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.repository.AccountRepository
import com.walletq.core.database.repository.BudgetRepository
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.datastore.PreferencesRepository
import com.walletq.core.model.Account
import com.walletq.core.model.Budget
import com.walletq.core.model.BudgetCalculationEngine
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Household
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.feature.activity.OwnershipFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val householdDao: HouseholdDao,
    private val profileDao: ProfileDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _ownershipFilter = MutableStateFlow(OwnershipFilter.ALL)
    private val _isAmountsHidden = MutableStateFlow(false)
    private val _streamError = MutableStateFlow<String?>(null)

    private val defaultProfileFlow = profileDao.getDefaultProfileFlow()
    private val activeProfileIdFlow = defaultProfileFlow.map { it?.id ?: "default-personal" }

    private val accessibleBudgetsFlow: Flow<List<Budget>> =
        activeProfileIdFlow.flatMapLatest { profileId ->
            budgetRepository.observeAccessibleBudgets(profileId)
        }

    private data class CoreHomeData(
        val accounts: List<Account>,
        val transactions: List<Transaction>,
        val budgets: List<Budget>,
        val categories: List<Category>,
        val households: List<Household>,
        val profileName: String,
        val defaultCurrencyCode: String
    )

    private val coreDataFlow: Flow<CoreHomeData> = combine(
        accountRepository.observeAllAccounts(),
        transactionRepository.getTransactions(),
        accessibleBudgetsFlow,
        categoryRepository.getCategories(),
        householdDao.getAllHouseholdsFlow().map { list -> list.map { it.toDomain() } },
        defaultProfileFlow.map { it?.name ?: "Personal" },
        preferencesRepository.userPreferencesFlow.map { it.defaultCurrencyCode }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoreHomeData(
            accounts = values[0] as List<Account>,
            transactions = values[1] as List<Transaction>,
            budgets = values[2] as List<Budget>,
            categories = values[3] as List<Category>,
            households = values[4] as List<Household>,
            profileName = values[5] as String,
            defaultCurrencyCode = values[6] as String
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreDataFlow,
        _selectedMonth,
        _ownershipFilter,
        _isAmountsHidden,
        _streamError
    ) { data, selectedMonth, ownershipFilter, isHidden, error ->
        val reportingCurrency = Currency.findByCode(data.defaultCurrencyCode)

        val scopedAccounts = data.accounts.filter { account ->
            !account.isArchived && matchesOwnership(account.ownershipType, ownershipFilter)
        }
        val reportingAccounts = scopedAccounts.filter { it.currency.code == reportingCurrency.code }
        val otherCurrencyCount = scopedAccounts.count { it.currency.code != reportingCurrency.code }
        val netWorthMinor = reportingAccounts.fold(0L) { acc, account ->
            Math.addExact(acc, account.currentBalanceMinorUnits)
        }

        val (monthStartMs, monthEndMs) = BudgetCalculationEngine.monthBoundaries(selectedMonth)
        val reversalIds = data.transactions.mapNotNull { it.reversalTransactionId }.toSet()

        val monthlyEligibleTxs = data.transactions.filter { tx ->
            tx.status == TransactionStatus.POSTED &&
                tx.id !in reversalIds &&
                tx.amount.currency.code == reportingCurrency.code &&
                tx.transactionDateEpochMs in monthStartMs..monthEndMs &&
                matchesOwnership(tx.ownershipType, ownershipFilter)
        }

        val monthlyIncome = monthlyEligibleTxs
            .asSequence()
            .filter { it.type == TransactionType.INCOME }
            .fold(0L) { acc, tx -> Math.addExact(acc, tx.amount.minorUnits) }

        val monthlyExpense = monthlyEligibleTxs
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .fold(0L) { acc, tx -> Math.addExact(acc, tx.amount.minorUnits) }

        val netCashFlow = Math.subtractExact(monthlyIncome, monthlyExpense)

        val activeBudgets = data.budgets
            .filter { budget ->
                !budget.isArchived &&
                    budget.startDateEpochMs <= monthEndMs &&
                    budget.endDateEpochMs >= monthStartMs &&
                    matchesOwnership(budget.ownershipType, ownershipFilter)
            }
            .map { budget ->
                BudgetCalculationEngine.calculateBudgetSpending(budget, data.transactions)
            }

        val recentTransactions = data.transactions
            .filter { tx -> matchesOwnership(tx.ownershipType, ownershipFilter) }
            .sortedWith(
                compareByDescending<Transaction> { it.transactionDateEpochMs }
                    .thenByDescending { it.createdAtEpochMs }
            )
            .take(5)

        HomeUiState(
            isLoading = false,
            errorMessage = error,
            profileName = data.profileName,
            selectedMonth = selectedMonth,
            ownershipFilter = ownershipFilter,
            isAmountsHidden = isHidden,
            reportingCurrency = reportingCurrency,
            netWorthMinorUnits = netWorthMinor,
            otherCurrencyAccountCount = otherCurrencyCount,
            monthlyIncomeMinorUnits = monthlyIncome,
            monthlyExpenseMinorUnits = monthlyExpense,
            netCashFlowMinorUnits = netCashFlow,
            accounts = scopedAccounts,
            accountsById = data.accounts.associateBy { it.id },
            householdsById = data.households.associateBy { it.id },
            categoriesById = data.categories.associateBy { it.id },
            activeBudgets = activeBudgets,
            recentTransactions = recentTransactions
        )
    }
        .catch { throwable ->
            _streamError.value = throwable.message ?: "Failed to load home dashboard."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun onPreviousMonth() {
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        _selectedMonth.update { it.plusMonths(1) }
    }

    fun onOwnershipFilterSelected(filter: OwnershipFilter) {
        _ownershipFilter.value = filter
    }

    fun onToggleAmountVisibility() {
        _isAmountsHidden.update { !it }
    }

    private fun matchesOwnership(type: OwnershipType, filter: OwnershipFilter): Boolean {
        return when (filter) {
            OwnershipFilter.ALL -> true
            OwnershipFilter.PERSONAL -> type == OwnershipType.PERSONAL
            OwnershipFilter.HOUSEHOLD -> type == OwnershipType.HOUSEHOLD
        }
    }
}
