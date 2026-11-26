package com.walletq.feature.accounts

import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.repository.AccountRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.datastore.PreferencesRepository
import com.walletq.core.model.Account
import com.walletq.core.model.AccountReconciliationEngine
import com.walletq.core.model.AccountReconciliationReport
import com.walletq.core.model.AccountType
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
class AccountViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeAccountRepository : AccountRepository {
        val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
        val createdAccounts = mutableListOf<Account>()

        override fun observeActiveAccounts(): Flow<List<Account>> =
            accountsFlow.map { list -> list.filter { !it.isArchived } }

        override fun observeAllAccounts(): Flow<List<Account>> = accountsFlow

        override fun observeAccountById(id: String): Flow<Account?> =
            accountsFlow.map { list -> list.find { it.id == id } }

        override suspend fun getAccountById(id: String): Account? =
            accountsFlow.value.find { it.id == id }

        override suspend fun createAccount(account: Account): Result<Account> {
            createdAccounts.add(account)
            accountsFlow.value = accountsFlow.value + account
            return Result.success(account)
        }

        override suspend fun updateAccountDetails(
            accountId: String,
            name: String,
            type: AccountType,
            description: String?,
            iconIdentifier: String?,
            colorIdentifier: String?
        ): Result<Account> {
            val existing = getAccountById(accountId) ?: return Result.failure(IllegalArgumentException("Not found"))
            val updated = existing.copy(
                name = name,
                type = type,
                description = description,
                iconIdentifier = iconIdentifier,
                colorIdentifier = colorIdentifier
            )
            accountsFlow.value = accountsFlow.value.map { if (it.id == accountId) updated else it }
            return Result.success(updated)
        }

        override suspend fun adjustBalance(
            accountId: String,
            targetBalanceMinorUnits: Long,
            reason: String?
        ): Result<Account> {
            val existing = getAccountById(accountId) ?: return Result.failure(IllegalArgumentException("Not found"))
            val updated = existing.copy(currentBalanceMinorUnits = targetBalanceMinorUnits)
            accountsFlow.value = accountsFlow.value.map { if (it.id == accountId) updated else it }
            return Result.success(updated)
        }

        override suspend fun archiveAccount(accountId: String): Result<Unit> {
            accountsFlow.value = accountsFlow.value.map {
                if (it.id == accountId) it.copy(isArchived = true) else it
            }
            return Result.success(Unit)
        }

        override suspend fun unarchiveAccount(accountId: String): Result<Unit> {
            accountsFlow.value = accountsFlow.value.map {
                if (it.id == accountId) it.copy(isArchived = false) else it
            }
            return Result.success(Unit)
        }

        override suspend fun deleteAccount(accountId: String): Result<Unit> = Result.success(Unit)

        override suspend fun calculateLedgerBalance(accountId: String): Long =
            getAccountById(accountId)?.currentBalanceMinorUnits ?: 0L

        override suspend fun reconcileAccount(accountId: String): Result<AccountReconciliationReport> {
            val acc = getAccountById(accountId)
                ?: return Result.failure(IllegalArgumentException("Not found"))
            return Result.success(AccountReconciliationEngine.reconcile(acc, emptyList(), emptyList()))
        }

        override fun observeAccountReconciliation(accountId: String): Flow<AccountReconciliationReport?> =
            accountsFlow.map { list ->
                list.find { it.id == accountId }?.let {
                    AccountReconciliationEngine.reconcile(it, emptyList(), emptyList())
                }
            }
    }

    private class FakeTransactionRepository : TransactionRepository {
        override suspend fun recordTransaction(transaction: Transaction): Result<Transaction> = Result.success(transaction)
        override suspend fun reverseTransaction(transactionId: String, reason: String?): Result<Transaction> =
            Result.failure(UnsupportedOperationException())
        override suspend fun replaceTransaction(
            originalTransactionId: String,
            replacement: Transaction,
            reason: String?
        ): Result<ReplacementResult> = Result.failure(UnsupportedOperationException())
        override suspend fun getTransactionById(id: String): Transaction? = null
        override fun getTransactionFlow(id: String): Flow<Transaction?> = flowOf(null)
        override fun getTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
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
        override suspend fun setDynamicColor(useDynamicColor: Boolean) {}
        override suspend fun setDefaultCurrency(currencyCode: String) {}
        override suspend fun setActiveProfileId(profileId: String?) {}
    }

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var viewModel: AccountViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accountRepository = FakeAccountRepository()
        viewModel = AccountViewModel(
            accountRepository = accountRepository,
            transactionRepository = FakeTransactionRepository(),
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
    fun currencySummaries_groupTotalsStrictlyByCurrency() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        accountRepository.accountsFlow.value = listOf(
            Account("a1", "default-personal", null, OwnershipType.PERSONAL, "Bank INR", AccountType.BANK, Currency.INR, 100000L, 150000L, false, 1000L),
            Account("a2", "default-personal", "hh-1", OwnershipType.HOUSEHOLD, "Cash INR", AccountType.CASH, Currency.INR, 50000L, 50000L, false, 1000L),
            Account("a3", "default-personal", null, OwnershipType.PERSONAL, "Forex USD", AccountType.BANK, Currency.USD, 2000L, 2000L, false, 1000L),
            Account("a4", "default-personal", null, OwnershipType.PERSONAL, "Old INR", AccountType.BANK, Currency.INR, 90000L, 90000L, true, 1000L)
        )

        val state = viewModel.uiState.value
        assertEquals(2, state.currencySummaries.size)
        val inrSummary = state.currencySummaries.find { it.currency == Currency.INR }
        val usdSummary = state.currencySummaries.find { it.currency == Currency.USD }
        assertNotNull(inrSummary)
        assertNotNull(usdSummary)
        assertEquals(200000L, inrSummary?.totalMinorUnits) // Archived excluded from active summary
        assertEquals(2, inrSummary?.accountCount)
        assertEquals(2000L, usdSummary?.totalMinorUnits)

        job.cancel()
    }

    @Test
    fun accountForm_validatesBlankNameAndCurrencyPrecision() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.openAddAccountSheet()
        viewModel.onAccountNameChanged("   ")
        viewModel.submitAccountForm()
        assertNotNull(viewModel.accountFormState.value.validationError)
        assertEquals(0, accountRepository.createdAccounts.size)

        // JPY has 0 decimal places -> 100.50 must fail
        viewModel.onAccountNameChanged("Tokyo Cash")
        viewModel.onAccountTypeChanged(AccountType.CASH)
        viewModel.onAccountCurrencyChanged("JPY")
        viewModel.onOpeningBalanceChanged("100.50")
        viewModel.submitAccountForm()
        assertTrue(viewModel.accountFormState.value.validationError?.contains("decimal") == true)
        assertEquals(0, accountRepository.createdAccounts.size)

        // Valid JPY whole units
        viewModel.onOpeningBalanceChanged("15000")
        viewModel.submitAccountForm()
        assertNull(viewModel.accountFormState.value.validationError)
        assertEquals(1, accountRepository.createdAccounts.size)
        assertEquals(15000L, accountRepository.createdAccounts.first().initialBalanceMinorUnits)

        job.cancel()
    }

    @Test
    fun filters_filterByOwnershipTypeAndArchived() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        accountRepository.accountsFlow.value = listOf(
            Account("a1", "default-personal", null, OwnershipType.PERSONAL, "Cash", AccountType.CASH, Currency.INR, 100L, 100L, false, 1000L),
            Account("a2", "default-personal", "hh-1", OwnershipType.HOUSEHOLD, "Joint Bank", AccountType.BANK, Currency.INR, 200L, 200L, false, 1000L),
            Account("a3", "default-personal", null, OwnershipType.PERSONAL, "Archived Card", AccountType.CREDIT_CARD, Currency.INR, 0L, 0L, true, 1000L)
        )

        assertEquals(2, viewModel.uiState.value.accounts.size)

        viewModel.onOwnershipFilterSelected(OwnershipFilter.HOUSEHOLD)
        assertEquals(1, viewModel.uiState.value.accounts.size)
        assertEquals("a2", viewModel.uiState.value.accounts.first().id)

        viewModel.clearFilters()
        viewModel.onShowArchivedToggled(true)
        assertEquals(3, viewModel.uiState.value.accounts.size)
        assertEquals(1, viewModel.uiState.value.archivedAccounts.size)

        job.cancel()
    }
}
