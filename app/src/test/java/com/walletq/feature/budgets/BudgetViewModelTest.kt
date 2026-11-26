package com.walletq.feature.budgets

import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.repository.BudgetRepository
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.datastore.PreferencesRepository
import com.walletq.core.model.Budget
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.ReplacementResult
import com.walletq.core.model.ThemeMode
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionType
import com.walletq.core.model.UserPreferences
import com.walletq.feature.activity.OwnershipFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeBudgetRepository : BudgetRepository {
        val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())
        val created = mutableListOf<Budget>()

        override fun observeAllBudgets(): Flow<List<Budget>> = budgetsFlow
        override fun observeAccessibleBudgets(profileId: String): Flow<List<Budget>> = budgetsFlow
        override fun observeBudgetById(budgetId: String): Flow<Budget?> =
            budgetsFlow.map { list -> list.find { it.id == budgetId } }
        override suspend fun getBudgetById(budgetId: String, requesterProfileId: String): Result<Budget> =
            budgetsFlow.value.find { it.id == budgetId }?.let { Result.success(it) }
                ?: Result.failure(IllegalArgumentException("Not found"))
        override suspend fun createBudget(budget: Budget, requesterProfileId: String): Result<Budget> {
            created.add(budget)
            budgetsFlow.value = budgetsFlow.value + budget
            return Result.success(budget)
        }
        override suspend fun updateBudget(budget: Budget, requesterProfileId: String): Result<Budget> {
            budgetsFlow.value = budgetsFlow.value.map { if (it.id == budget.id) budget else it }
            return Result.success(budget)
        }
        override suspend fun archiveBudget(budgetId: String, requesterProfileId: String): Result<Unit> {
            budgetsFlow.value = budgetsFlow.value.map { if (it.id == budgetId) it.copy(isArchived = true) else it }
            return Result.success(Unit)
        }
        override suspend fun unarchiveBudget(budgetId: String, requesterProfileId: String): Result<Unit> {
            budgetsFlow.value = budgetsFlow.value.map { if (it.id == budgetId) it.copy(isArchived = false) else it }
            return Result.success(Unit)
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val txsFlow = MutableStateFlow<List<Transaction>>(emptyList())
        override suspend fun recordTransaction(transaction: Transaction): Result<Transaction> = Result.success(transaction)
        override suspend fun reverseTransaction(transactionId: String, reason: String?): Result<Transaction> = Result.failure(UnsupportedOperationException())
        override suspend fun replaceTransaction(originalTransactionId: String, replacement: Transaction, reason: String?): Result<ReplacementResult> = Result.failure(UnsupportedOperationException())
        override suspend fun getTransactionById(id: String): Transaction? = null
        override fun getTransactionFlow(id: String): Flow<Transaction?> = flowOf(null)
        override fun getTransactions(): Flow<List<Transaction>> = txsFlow
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

    private class FakeCategoryRepository : CategoryRepository {
        private val cats = listOf(
            Category("cat-food", "Food", TransactionType.EXPENSE, "restaurant", "#FF5722", isSystemCategory = true),
            Category("cat-salary", "Salary", TransactionType.INCOME, "payments", "#4CAF50", isSystemCategory = true)
        )
        override fun getCategories(): Flow<List<Category>> = flowOf(cats)
        override fun getActiveCategories(): Flow<List<Category>> = flowOf(cats)
        override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = flowOf(cats.filter { it.transactionType == type })
        override fun getCategoriesByProfile(profileId: String): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByHousehold(householdId: String): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: String): Category? = cats.find { it.id == id }
        override suspend fun createCategory(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun updateCategory(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun archiveCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun unarchiveCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteCategory(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeHouseholdDao : HouseholdDao {
        val households = listOf(HouseholdEntity("hh-1", "Family", "Shared", 1000L))
        override suspend fun insert(household: HouseholdEntity) {}
        override suspend fun update(household: HouseholdEntity) {}
        override suspend fun delete(household: HouseholdEntity) {}
        override suspend fun insertMember(member: HouseholdMemberEntity) {}
        override suspend fun deleteMember(householdId: String, profileId: String) {}
        override suspend fun getById(id: String): HouseholdEntity? = households.firstOrNull { it.id == id }
        override fun getAllHouseholdsFlow(): Flow<List<HouseholdEntity>> = flowOf(households)
        override fun getMembersForHouseholdFlow(householdId: String): Flow<List<HouseholdMemberEntity>> = flowOf(emptyList())
        override suspend fun getMembersForHousehold(householdId: String): List<HouseholdMemberEntity> = emptyList()
        override fun getHouseholdsForProfileFlow(profileId: String): Flow<List<HouseholdEntity>> = flowOf(households)
        override suspend fun getMemberCount(householdId: String): Int = 1
        override suspend fun getHouseholdCount(): Int = 1
    }

    private class FakeProfileDao : ProfileDao {
        val defaultProfile = ProfileEntity("default-personal", "Personal", OwnershipType.PERSONAL, true, 1000L)
        override suspend fun insert(profile: ProfileEntity) {}
        override suspend fun update(profile: ProfileEntity) {}
        override suspend fun delete(profile: ProfileEntity) {}
        override suspend fun getById(id: String): ProfileEntity? = defaultProfile
        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flowOf(listOf(defaultProfile))
        override fun getDefaultProfileFlow(): Flow<ProfileEntity?> = flowOf(defaultProfile)
        override suspend fun getCount(): Int = 1
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override val userPreferencesFlow: Flow<UserPreferences> = flowOf(UserPreferences())
        override suspend fun setThemeMode(themeMode: ThemeMode) {}
        override suspend fun setDynamicColor(enabled: Boolean) {}
        override suspend fun setDefaultCurrency(currencyCode: String) {}
        override suspend fun setActiveProfileId(profileId: String?) {}
    }

    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var viewModel: BudgetViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        budgetRepository = FakeBudgetRepository()
        viewModel = BudgetViewModel(
            budgetRepository = budgetRepository,
            transactionRepository = FakeTransactionRepository(),
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
    fun createBudget_validatesCurrencyPrecisionAndDuplicateCategories() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // Only expense categories exposed in UI state
        assertEquals(listOf("cat-food"), viewModel.uiState.value.expenseCategories.map { it.id })

        viewModel.openCreateBudgetSheet()
        viewModel.onNameChanged("Tokyo Trip")
        viewModel.onCurrencyChanged("JPY")
        viewModel.onAllocatedAmountChanged("1000.50") // Invalid for JPY
        viewModel.submitBudgetForm()
        assertNotNull(viewModel.formState.value.validationError)
        assertEquals(0, budgetRepository.created.size)

        // Valid JPY whole units + category allocation
        viewModel.onAllocatedAmountChanged("50000")
        viewModel.addCategoryAllocation("cat-food")
        viewModel.addCategoryAllocation("cat-food") // Duplicate rejected
        assertNotNull(viewModel.formState.value.validationError)

        val allocId = viewModel.formState.value.categoryAllocations.first().id
        viewModel.updateCategoryAllocationAmount(allocId, "20000")
        viewModel.submitBudgetForm()

        assertNull(viewModel.formState.value.validationError)
        assertEquals(1, budgetRepository.created.size)
        assertEquals(50000L, budgetRepository.created.first().allocatedAmountMinorUnits)

        job.cancel()
    }

    @Test
    fun filters_filterByOwnershipAndArchived() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        budgetRepository.budgetsFlow.value = listOf(
            Budget("b1", "Personal", startDateEpochMs = 1000L, endDateEpochMs = 2000L, currency = Currency.INR, allocatedAmountMinorUnits = 10000L, ownershipType = OwnershipType.PERSONAL, profileId = "default-personal", createdAtEpochMs = 1000L, updatedAtEpochMs = 1000L),
            Budget("b2", "Household", startDateEpochMs = 1000L, endDateEpochMs = 2000L, currency = Currency.INR, allocatedAmountMinorUnits = 20000L, ownershipType = OwnershipType.HOUSEHOLD, householdId = "hh-1", createdAtEpochMs = 1000L, updatedAtEpochMs = 1000L),
            Budget("b3", "Archived", startDateEpochMs = 1000L, endDateEpochMs = 2000L, currency = Currency.INR, allocatedAmountMinorUnits = 5000L, ownershipType = OwnershipType.PERSONAL, profileId = "default-personal", isArchived = true, createdAtEpochMs = 1000L, updatedAtEpochMs = 1000L)
        )

        assertEquals(2, viewModel.uiState.value.budgetSummaries.size)

        viewModel.onOwnershipFilterSelected(OwnershipFilter.HOUSEHOLD)
        assertEquals(listOf("b2"), viewModel.uiState.value.budgetSummaries.map { it.budget.id })

        viewModel.onOwnershipFilterSelected(OwnershipFilter.ALL)
        viewModel.onShowArchivedToggled(true)
        assertEquals(3, viewModel.uiState.value.budgetSummaries.size)
        assertTrue(viewModel.uiState.value.budgetSummaries.any { it.budget.isArchived })

        job.cancel()
    }
}
