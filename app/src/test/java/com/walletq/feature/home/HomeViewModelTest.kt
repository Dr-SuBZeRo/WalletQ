package com.walletq.feature.home

import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.repository.AccountRepository
import com.walletq.core.database.repository.BudgetRepository
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.datastore.PreferencesRepository
import com.walletq.core.model.Account
import com.walletq.core.model.AccountReconciliationReport
import com.walletq.core.model.AccountType
import com.walletq.core.model.Budget
import com.walletq.core.model.BudgetCalculationEngine
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.ReplacementResult
import com.walletq.core.model.ThemeMode
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.core.model.TransferDetails
import com.walletq.core.model.UserPreferences
import com.walletq.feature.activity.OwnershipFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeAccountRepository : AccountRepository {
        val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
        override fun observeActiveAccounts(): Flow<List<Account>> = accountsFlow
        override fun observeAllAccounts(): Flow<List<Account>> = accountsFlow
        override fun observeAccountById(id: String): Flow<Account?> = flowOf(null)
        override suspend fun getAccountById(id: String): Account? = null
        override suspend fun createAccount(account: Account): Result<Account> = Result.success(account)
        override suspend fun updateAccountDetails(accountId: String, name: String, type: AccountType, description: String?, iconIdentifier: String?, colorIdentifier: String?): Result<Account> = Result.failure(UnsupportedOperationException())
        override suspend fun adjustBalance(accountId: String, targetBalanceMinorUnits: Long, reason: String?): Result<Account> = Result.failure(UnsupportedOperationException())
        override suspend fun archiveAccount(accountId: String): Result<Unit> = Result.success(Unit)
        override suspend fun unarchiveAccount(accountId: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAccount(accountId: String): Result<Unit> = Result.success(Unit)
        override suspend fun calculateLedgerBalance(accountId: String): Long = 0L
        override suspend fun reconcileAccount(accountId: String): Result<AccountReconciliationReport> = Result.failure(UnsupportedOperationException())
        override fun observeAccountReconciliation(accountId: String): Flow<AccountReconciliationReport?> = flowOf(null)
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
        override suspend fun recordTransaction(transaction: Transaction): Result<Transaction> = Result.success(transaction)
        override suspend fun reverseTransaction(transactionId: String, reason: String?): Result<Transaction> = Result.failure(UnsupportedOperationException())
        override suspend fun replaceTransaction(originalTransactionId: String, replacement: Transaction, reason: String?): Result<ReplacementResult> = Result.failure(UnsupportedOperationException())
        override suspend fun getTransactionById(id: String): Transaction? = null
        override fun getTransactionFlow(id: String): Flow<Transaction?> = flowOf(null)
        override fun getTransactions(): Flow<List<Transaction>> = transactionsFlow
        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByProfile(profileId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByHousehold(householdId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
        override fun searchTransactions(query: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getLedgerEntriesForAccount(accountId: String): Flow<List<LedgerEntry>> = flowOf(emptyList())
        override fun getLedgerEntriesForTransaction(transactionId: String): Flow<List<LedgerEntry>> = flowOf(emptyList())
        override suspend fun calculateAccountBalance(accountId: String): Long = 0L
        override suspend fun calculateBalanceAtTimestamp(accountId: String, timestampEpochMs: Long): Long = 0L
    }

    private class FakeBudgetRepository : BudgetRepository {
        val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())
        override fun observeAllBudgets(): Flow<List<Budget>> = budgetsFlow
        override fun observeAccessibleBudgets(profileId: String): Flow<List<Budget>> = budgetsFlow
        override fun observeBudgetById(budgetId: String): Flow<Budget?> = flowOf(null)
        override suspend fun getBudgetById(budgetId: String, requesterProfileId: String): Result<Budget> = Result.failure(UnsupportedOperationException())
        override suspend fun createBudget(budget: Budget, requesterProfileId: String): Result<Budget> = Result.success(budget)
        override suspend fun updateBudget(budget: Budget, requesterProfileId: String): Result<Budget> = Result.success(budget)
        override suspend fun archiveBudget(budgetId: String, requesterProfileId: String): Result<Unit> = Result.success(Unit)
        override suspend fun unarchiveBudget(budgetId: String, requesterProfileId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun getCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getActiveCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByProfile(profileId: String): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByHousehold(householdId: String): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: String): Category? = null
        override suspend fun createCategory(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun updateCategory(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun archiveCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun unarchiveCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteCategory(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeHouseholdDao : HouseholdDao {
        override suspend fun insert(household: HouseholdEntity) {}
        override suspend fun update(household: HouseholdEntity) {}
        override suspend fun delete(household: HouseholdEntity) {}
        override suspend fun insertMember(member: HouseholdMemberEntity) {}
        override suspend fun deleteMember(householdId: String, profileId: String) {}
        override suspend fun getById(id: String): HouseholdEntity? = null
        override fun getAllHouseholdsFlow(): Flow<List<HouseholdEntity>> = flowOf(emptyList())
        override fun getMembersForHouseholdFlow(householdId: String): Flow<List<HouseholdMemberEntity>> = flowOf(emptyList())
        override suspend fun getMembersForHousehold(householdId: String): List<HouseholdMemberEntity> = emptyList()
        override fun getHouseholdsForProfileFlow(profileId: String): Flow<List<HouseholdEntity>> = flowOf(emptyList())
        override suspend fun getMemberCount(householdId: String): Int = 0
        override suspend fun getHouseholdCount(): Int = 0
    }

    private class FakeProfileDao : ProfileDao {
        private val defaultProfile = ProfileEntity("default-personal", "Personal", OwnershipType.PERSONAL, true, 1000L)
        override suspend fun insert(profile: ProfileEntity) {}
        override suspend fun update(profile: ProfileEntity) {}
        override suspend fun delete(profile: ProfileEntity) {}
        override suspend fun getById(id: String): ProfileEntity? = defaultProfile
        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flowOf(listOf(defaultProfile))
        override fun getDefaultProfileFlow(): Flow<ProfileEntity?> = flowOf(defaultProfile)
        override suspend fun getCount(): Int = 1
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override val userPreferencesFlow: Flow<UserPreferences> = flowOf(UserPreferences(defaultCurrencyCode = "INR"))
        override suspend fun setThemeMode(themeMode: ThemeMode) {}
        override suspend fun setDynamicColor(enabled: Boolean) {}
        override suspend fun setDefaultCurrency(currencyCode: String) {}
        override suspend fun setActiveProfileId(profileId: String?) {}
    }

    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var txRepo: FakeTransactionRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accountRepo = FakeAccountRepository()
        txRepo = FakeTransactionRepository()
        viewModel = HomeViewModel(
            accountRepository = accountRepo,
            transactionRepository = txRepo,
            budgetRepository = FakeBudgetRepository(),
            categoryRepository = FakeCategoryRepository(),
            householdDao = FakeHouseholdDao(),
            profileDao = FakeProfileDao(),
            preferencesRepository = FakePreferencesRepository()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun homeDashboard_computesNetWorthMonthlyCashFlowAndIsolatesCurrencies() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val (monthStart, _) = BudgetCalculationEngine.monthBoundaries(YearMonth.now())

        accountRepo.accountsFlow.value = listOf(
            Account("a1", "default-personal", null, OwnershipType.PERSONAL, "Bank", AccountType.BANK, Currency.INR, 100000L, 150000L, false, 1000L),
            Account("a2", "default-personal", "hh-1", OwnershipType.HOUSEHOLD, "Joint", AccountType.CASH, Currency.INR, 50000L, 50000L, false, 1000L),
            Account("a3", "default-personal", null, OwnershipType.PERSONAL, "USD Forex", AccountType.BANK, Currency.USD, 1000L, 1000L, false, 1000L)
        )

        txRepo.transactionsFlow.value = listOf(
            Transaction("t-inc", TransactionType.INCOME, Money(80000L, Currency.INR), "a1", null, monthStart + 1000L, "Salary", null, OwnershipType.PERSONAL, "default-personal", null, null, TransactionStatus.POSTED, null, null, monthStart, monthStart),
            Transaction("t-exp", TransactionType.EXPENSE, Money(30000L, Currency.INR), "a1", null, monthStart + 2000L, "Groceries", null, OwnershipType.PERSONAL, "default-personal", null, null, TransactionStatus.POSTED, null, null, monthStart, monthStart),
            Transaction("t-trf", TransactionType.TRANSFER, Money(10000L, Currency.INR), "a1", null, monthStart + 3000L, "Transfer", null, OwnershipType.PERSONAL, "default-personal", null, TransferDetails("a2", Money(10000L, Currency.INR)), TransactionStatus.POSTED, null, null, monthStart, monthStart),
            Transaction("t-rev", TransactionType.EXPENSE, Money(5000L, Currency.INR), "a1", null, monthStart + 4000L, "Reversed", null, OwnershipType.PERSONAL, "default-personal", null, null, TransactionStatus.REVERSED, null, "t-rev-comp", monthStart, monthStart),
            Transaction("t-rev-comp", TransactionType.EXPENSE, Money(5000L, Currency.INR), "a1", null, monthStart + 5000L, "Reversal", null, OwnershipType.PERSONAL, "default-personal", null, null, TransactionStatus.POSTED, "t-rev", null, monthStart, monthStart)
        )

        val state = viewModel.uiState.value
        assertEquals(200000L, state.netWorthMinorUnits) // Only INR accounts summed
        assertEquals(1, state.otherCurrencyAccountCount) // USD kept separate
        assertEquals(80000L, state.monthlyIncomeMinorUnits)
        assertEquals(30000L, state.monthlyExpenseMinorUnits) // Transfer, reversed, and reversal excluded
        assertEquals(50000L, state.netCashFlowMinorUnits)

        // Ownership filter
        viewModel.onOwnershipFilterSelected(OwnershipFilter.PERSONAL)
        assertEquals(150000L, viewModel.uiState.value.netWorthMinorUnits)

        // Visibility toggle
        assertFalse(viewModel.uiState.value.isAmountsHidden)
        viewModel.onToggleAmountVisibility()
        assertTrue(viewModel.uiState.value.isAmountsHidden)

        job.cancel()
    }
}
