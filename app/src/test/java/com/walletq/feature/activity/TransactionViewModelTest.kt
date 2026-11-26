package com.walletq.feature.activity

import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.ExchangeRateDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.ExchangeRateEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.model.AccountType
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.LedgerEntryType
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.ReplacementResult
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeTransactionRepository : TransactionRepository {
        val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
        val ledgerEntriesFlow = MutableStateFlow<List<LedgerEntry>>(emptyList())
        val recordedTransactions = mutableListOf<Transaction>()
        val reversedIds = mutableListOf<Pair<String, String?>>()
        var pendingRecordGate: CompletableDeferred<Unit>? = null
        var nextRecordError: Throwable? = null

        override suspend fun recordTransaction(transaction: Transaction): Result<Transaction> {
            pendingRecordGate?.await()
            nextRecordError?.let { return Result.failure(it) }
            recordedTransactions.add(transaction)
            transactionsFlow.value = listOf(transaction) + transactionsFlow.value
            return Result.success(transaction)
        }

        override suspend fun reverseTransaction(transactionId: String, reason: String?): Result<Transaction> {
            reversedIds.add(transactionId to reason)
            val original = transactionsFlow.value.find { it.id == transactionId }
                ?: return Result.failure(IllegalArgumentException("Not found"))
            val updatedOriginal = original.copy(
                status = TransactionStatus.REVERSED,
                reversalTransactionId = "rev-$transactionId"
            )
            val reversalTx = original.copy(
                id = "rev-$transactionId",
                originalTransactionId = transactionId,
                description = "Reversal: ${original.description}"
            )
            transactionsFlow.value = transactionsFlow.value.map {
                if (it.id == transactionId) updatedOriginal else it
            } + reversalTx
            return Result.success(reversalTx)
        }

        override suspend fun replaceTransaction(
            originalTransactionId: String,
            replacement: Transaction,
            reason: String?
        ): Result<ReplacementResult> = Result.failure(UnsupportedOperationException())

        override suspend fun getTransactionById(id: String): Transaction? =
            transactionsFlow.value.find { it.id == id }

        override fun getTransactionFlow(id: String): Flow<Transaction?> =
            transactionsFlow.map { list -> list.find { it.id == id } }

        override fun getTransactions(): Flow<List<Transaction>> = transactionsFlow
        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByProfile(profileId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByHousehold(householdId: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
        override fun searchTransactions(query: String): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getLedgerEntriesForAccount(accountId: String): Flow<List<LedgerEntry>> = flowOf(emptyList())
        override fun getLedgerEntriesForTransaction(transactionId: String): Flow<List<LedgerEntry>> =
            ledgerEntriesFlow.map { list -> list.filter { it.transactionId == transactionId } }
        override suspend fun calculateAccountBalance(accountId: String): Long = 0L
        override suspend fun calculateBalanceAtTimestamp(accountId: String, timestampEpochMs: Long): Long = 0L
    }

    private class FakeCategoryRepository : CategoryRepository {
        val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
        override fun getCategories(): Flow<List<Category>> = categoriesFlow
        override fun getActiveCategories(): Flow<List<Category>> = categoriesFlow.map { it.filter { c -> !c.isArchived } }
        override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByProfile(profileId: String): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByHousehold(householdId: String): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: String): Category? = categoriesFlow.value.find { it.id == id }
        override suspend fun createCategory(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun updateCategory(category: Category): Result<Unit> = Result.success(Unit)
        override suspend fun archiveCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun unarchiveCategory(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteCategory(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeAccountDao : AccountDao {
        val accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
        override suspend fun insert(account: AccountEntity) {}
        override suspend fun update(account: AccountEntity) {}
        override suspend fun delete(account: AccountEntity) {}
        override suspend fun getById(id: String): AccountEntity? = accountsFlow.value.find { it.id == id }
        override fun getAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> = flowOf(emptyList())
        override fun getAccountsByHouseholdFlow(householdId: String): Flow<List<AccountEntity>> = flowOf(emptyList())
        override fun getPersonalAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> = flowOf(emptyList())
        override fun getAllActiveAccountsFlow(): Flow<List<AccountEntity>> = accountsFlow.map { it.filter { a -> !a.isArchived } }
        override fun observeAllAccountsFlow(): Flow<List<AccountEntity>> = accountsFlow
        override fun observeAccountByIdFlow(id: String): Flow<AccountEntity?> = accountsFlow.map { list -> list.find { it.id == id } }
        override suspend fun findByNameInPersonalScope(profileId: String, name: String): AccountEntity? = null
        override suspend fun findByNameInHouseholdScope(householdId: String, name: String): AccountEntity? = null
        override suspend fun getCount(): Int = accountsFlow.value.size
        override suspend fun updateCurrentBalance(id: String, balance: Long) {}
        override suspend fun setArchived(id: String, isArchived: Boolean) {}
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

    private class FakeExchangeRateDao : ExchangeRateDao {
        val rates = mutableMapOf<Pair<String, String>, ExchangeRateEntity>()
        override suspend fun insert(rate: ExchangeRateEntity) {
            rates[rate.baseCurrencyCode to rate.targetCurrencyCode] = rate
        }
        override suspend fun insertAll(rates: List<ExchangeRateEntity>) {}
        override suspend fun getLatestRate(baseCode: String, targetCode: String): ExchangeRateEntity? =
            rates[baseCode to targetCode]
        override fun getRateHistoryFlow(baseCode: String, targetCode: String): Flow<List<ExchangeRateEntity>> = flowOf(emptyList())
        override fun getAllRatesFlow(): Flow<List<ExchangeRateEntity>> = flowOf(emptyList())
    }

    private lateinit var txRepo: FakeTransactionRepository
    private lateinit var catRepo: FakeCategoryRepository
    private lateinit var accountDao: FakeAccountDao
    private lateinit var profileDao: FakeProfileDao
    private lateinit var householdDao: FakeHouseholdDao
    private lateinit var exchangeRateDao: FakeExchangeRateDao
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        txRepo = FakeTransactionRepository()
        catRepo = FakeCategoryRepository()
        accountDao = FakeAccountDao()
        profileDao = FakeProfileDao()
        householdDao = FakeHouseholdDao()
        exchangeRateDao = FakeExchangeRateDao()

        accountDao.accountsFlow.value = listOf(
            AccountEntity(
                id = "acc-inr",
                profileId = "default-personal",
                householdId = null,
                ownershipType = OwnershipType.PERSONAL,
                name = "Main INR",
                type = AccountType.BANK,
                currencyCode = "INR",
                initialBalanceMinorUnits = 100000L,
                currentBalanceMinorUnits = 100000L,
                isArchived = false,
                createdAtEpochMs = 1000L
            ),
            AccountEntity(
                id = "acc-jpy",
                profileId = "default-personal",
                householdId = null,
                ownershipType = OwnershipType.PERSONAL,
                name = "Travel JPY",
                type = AccountType.CASH,
                currencyCode = "JPY",
                initialBalanceMinorUnits = 50000L,
                currentBalanceMinorUnits = 50000L,
                isArchived = false,
                createdAtEpochMs = 1000L
            ),
            AccountEntity(
                id = "acc-hh-inr",
                profileId = "default-personal",
                householdId = "hh-1",
                ownershipType = OwnershipType.HOUSEHOLD,
                name = "Family Joint",
                type = AccountType.BANK,
                currencyCode = "INR",
                initialBalanceMinorUnits = 500000L,
                currentBalanceMinorUnits = 500000L,
                isArchived = false,
                createdAtEpochMs = 1000L
            )
        )

        catRepo.categoriesFlow.value = listOf(
            Category("cat-food", "Food", TransactionType.EXPENSE, "restaurant", "#FF5722", isSystemCategory = true),
            Category("cat-salary", "Salary", TransactionType.INCOME, "work", "#4CAF50", isSystemCategory = true),
            Category("cat-personal-hobby", "Hobby", TransactionType.EXPENSE, "spa", "#009688", profileId = "default-personal", isSystemCategory = false),
            Category("cat-hh-rent", "Household Rent", TransactionType.EXPENSE, "home", "#795548", householdId = "hh-1", isSystemCategory = false)
        )

        viewModel = TransactionViewModel(
            transactionRepository = txRepo,
            categoryRepository = catRepo,
            accountDao = accountDao,
            profileDao = profileDao,
            householdDao = householdDao,
            exchangeRateDao = exchangeRateDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun incomeAndExpenseFormValidation_rejectsZeroNegativeDecimalMismatchAndBlankDescription() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.openAddTransactionSheet(TransactionType.EXPENSE)
        viewModel.onFormAccountSelected("acc-jpy")
        viewModel.onFormDescriptionChanged("Sushi")

        // JPY cannot have decimals
        viewModel.onFormAmountChanged("500.5")
        viewModel.submitIncomeOrExpense()
        assertEquals("JPY does not support decimal places.", viewModel.transactionFormState.value.validationError)
        assertTrue(txRepo.recordedTransactions.isEmpty())

        // Zero amount rejected
        viewModel.onFormAmountChanged("0")
        viewModel.submitIncomeOrExpense()
        assertEquals("Amount must be greater than zero.", viewModel.transactionFormState.value.validationError)

        // Blank description rejected
        viewModel.onFormAmountChanged("1500")
        viewModel.onFormDescriptionChanged("   ")
        viewModel.submitIncomeOrExpense()
        assertEquals("Please enter a description.", viewModel.transactionFormState.value.validationError)

        // Valid submission succeeds
        viewModel.onFormDescriptionChanged("Tokyo Lunch")
        viewModel.submitIncomeOrExpense()
        assertEquals(1, txRepo.recordedTransactions.size)
        assertEquals(1500L, txRepo.recordedTransactions[0].amount.minorUnits)
        assertEquals(Currency.JPY, txRepo.recordedTransactions[0].amount.currency)
        assertEquals(ActiveActivitySheet.NONE, viewModel.uiState.value.activeSheet)

    }

    @Test
    fun householdOwnership_selectionAlignsAccountAndCategoryValidation() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.openAddTransactionSheet(TransactionType.EXPENSE)
        viewModel.onFormOwnershipContextChanged(OwnershipType.HOUSEHOLD)

        val form = viewModel.transactionFormState.value
        assertEquals(OwnershipType.HOUSEHOLD, form.ownershipContext)
        assertEquals("acc-hh-inr", form.selectedAccountId)

        // Household category is valid for household account, personal-only category is invalid
        val hhAccount = viewModel.uiState.value.accountsById["acc-hh-inr"]!!
        val personalCat = viewModel.uiState.value.categoriesById["cat-personal-hobby"]!!
        val hhCat = viewModel.uiState.value.categoriesById["cat-hh-rent"]!!
        assertFalse(viewModel.isCategoryValidForAccount(personalCat, hhAccount))
        assertTrue(viewModel.isCategoryValidForAccount(hhCat, hhAccount))

        viewModel.onFormCategorySelected("cat-hh-rent")
        viewModel.onFormAmountChanged("12000.00")
        viewModel.onFormDescriptionChanged("Monthly Rent")
        viewModel.submitIncomeOrExpense()

        assertEquals(1, txRepo.recordedTransactions.size)
        val recorded = txRepo.recordedTransactions[0]
        assertEquals(OwnershipType.HOUSEHOLD, recorded.ownershipType)
        assertEquals("hh-1", recorded.householdId)
        assertEquals("cat-hh-rent", recorded.categoryId)

    }

    @Test
    fun transferValidation_preventsSameAccountAndMissingExchangeRate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.openTransferSheet()
        viewModel.onTransferSourceAccountSelected("acc-inr")
        viewModel.onTransferDestinationAccountSelected("acc-inr")
        viewModel.onTransferAmountChanged("100.00")
        viewModel.submitTransfer()

        assertEquals("Source and destination accounts must be different.", viewModel.transferFormState.value.validationError)
        assertTrue(txRepo.recordedTransactions.isEmpty())

        // Cross-currency without rate fails
        viewModel.onTransferDestinationAccountSelected("acc-jpy")
        assertTrue(viewModel.transferFormState.value.isCrossCurrency)
        viewModel.submitTransfer()

        assertEquals("Exchange rate is required for cross-currency transfers.", viewModel.transferFormState.value.validationError)
        assertTrue(txRepo.recordedTransactions.isEmpty())

        // Enter manual rate: 1 INR = 1.85 JPY => 100.00 INR = 185 JPY
        viewModel.onTransferExchangeRateChanged("1.85")
        assertEquals(185L, viewModel.transferFormState.value.destinationPreview?.minorUnits)

        viewModel.submitTransfer()
        assertEquals(1, txRepo.recordedTransactions.size)
        val transfer = txRepo.recordedTransactions[0]
        assertEquals(TransactionType.TRANSFER, transfer.type)
        assertEquals(10000L, transfer.amount.minorUnits)
        assertEquals(185L, transfer.transferDetails?.destinationAmount?.minorUnits)
        assertEquals(0, BigDecimal("1.85").compareTo(transfer.transferDetails?.appliedExchangeRate))

    }

    @Test
    fun duplicateSubmissionPrevention_blocksConcurrentSubmitCalls() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val gate = CompletableDeferred<Unit>()
        txRepo.pendingRecordGate = gate

        viewModel.openAddTransactionSheet(TransactionType.INCOME)
        viewModel.onFormAccountSelected("acc-inr")
        viewModel.onFormAmountChanged("5000.00")
        viewModel.onFormDescriptionChanged("Salary")

        // First submit starts and suspends on gate
        viewModel.submitIncomeOrExpense()
        assertTrue(viewModel.transactionFormState.value.isSubmitting)

        // Second rapid tap while submitting must be ignored
        viewModel.submitIncomeOrExpense()

        gate.complete(Unit)
        assertFalse(viewModel.transactionFormState.value.isSubmitting)
        assertEquals(1, txRepo.recordedTransactions.size)

    }

    @Test
    fun reversalWorkflow_reversesPostedTransaction() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val tx = Transaction(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amount = Money(25000L, Currency.INR),
            accountId = "acc-inr",
            categoryId = "cat-food",
            transactionDateEpochMs = System.currentTimeMillis(),
            description = "Dinner",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "default-personal",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        txRepo.transactionsFlow.value = listOf(tx)
        txRepo.ledgerEntriesFlow.value = listOf(
            LedgerEntry("le-1", "tx-1", "acc-inr", -25000L, Currency.INR, LedgerEntryType.DEBIT, 1000L)
        )

        viewModel.openTransactionDetails("tx-1")
        assertNotNull(viewModel.uiState.value.selectedTransaction)
        assertEquals(1, viewModel.uiState.value.selectedTransactionLedgerEntries.size)

        viewModel.reverseSelectedTransaction("Duplicate charge")
        assertEquals(1, txRepo.reversedIds.size)
        assertEquals("tx-1" to "Duplicate charge", txRepo.reversedIds[0])
        assertEquals(TransactionStatus.REVERSED, viewModel.uiState.value.selectedTransaction?.status)

        // Calling reversal again on REVERSED transaction is a no-op
        viewModel.reverseSelectedTransaction("Again")
        assertEquals(1, txRepo.reversedIds.size)

    }

    @Test
    fun transactionHistoryFilters_filterBySearchTypeOwnershipAccountAndDate() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val now = System.currentTimeMillis()
        val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000)

        val t1 = Transaction(
            id = "t1",
            type = TransactionType.EXPENSE,
            amount = Money(5000L, Currency.INR),
            accountId = "acc-inr",
            categoryId = "cat-food",
            transactionDateEpochMs = now,
            description = "Coffee Shop",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "default-personal",
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val t2 = Transaction(
            id = "t2",
            type = TransactionType.INCOME,
            amount = Money(100000L, Currency.INR),
            accountId = "acc-hh-inr",
            categoryId = "cat-salary",
            transactionDateEpochMs = tenDaysAgo,
            description = "Household Bonus",
            ownershipType = OwnershipType.HOUSEHOLD,
            ownerProfileId = "default-personal",
            householdId = "hh-1",
            createdAtEpochMs = tenDaysAgo,
            updatedAtEpochMs = tenDaysAgo
        )
        txRepo.transactionsFlow.value = listOf(t1, t2)

        assertEquals(2, viewModel.uiState.value.transactions.size)

        // Search filter
        viewModel.onSearchQueryChanged("Coffee")
        assertEquals(listOf("t1"), viewModel.uiState.value.transactions.map { it.id })
        viewModel.clearFilters()

        // Ownership filter
        viewModel.onOwnershipFilterChanged(OwnershipFilter.HOUSEHOLD)
        assertEquals(listOf("t2"), viewModel.uiState.value.transactions.map { it.id })
        viewModel.clearFilters()

        // Type filter
        viewModel.onTypeFilterChanged(TransactionType.INCOME)
        assertEquals(listOf("t2"), viewModel.uiState.value.transactions.map { it.id })
        viewModel.clearFilters()

        // Account filter
        viewModel.onAccountFilterChanged("acc-inr")
        assertEquals(listOf("t1"), viewModel.uiState.value.transactions.map { it.id })
        viewModel.clearFilters()

        // Date filter (TODAY excludes tenDaysAgo and future-dated transactions)
        val twoDaysFuture = now + (2L * 24 * 60 * 60 * 1000)
        val tFuture = t1.copy(id = "t3", transactionDateEpochMs = twoDaysFuture, description = "Future Tx")
        txRepo.transactionsFlow.value = listOf(t1, t2, tFuture)

        viewModel.onDateRangeFilterChanged(DateRangeFilter.TODAY)
        assertEquals(listOf("t1"), viewModel.uiState.value.transactions.map { it.id })
    }

    @Test
    fun archivedAccounts_excludedFromSelectorsButPreservedInAccountsById() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        accountDao.accountsFlow.value = listOf(
            AccountEntity(
                id = "acc-active",
                profileId = "default-personal",
                ownershipType = OwnershipType.PERSONAL,
                name = "Active Bank",
                type = AccountType.BANK,
                currencyCode = "INR",
                initialBalanceMinorUnits = 10000L,
                currentBalanceMinorUnits = 10000L,
                isArchived = false,
                createdAtEpochMs = 1000L
            ),
            AccountEntity(
                id = "acc-archived",
                profileId = "default-personal",
                ownershipType = OwnershipType.PERSONAL,
                name = "Archived Wallet",
                type = AccountType.CASH,
                currencyCode = "INR",
                initialBalanceMinorUnits = 5000L,
                currentBalanceMinorUnits = 5000L,
                isArchived = true,
                createdAtEpochMs = 1000L
            )
        )

        val state = viewModel.uiState.value
        assertEquals(listOf("acc-active"), state.accounts.map { it.id })
        assertNotNull(state.accountsById["acc-archived"])
        assertEquals("Archived Wallet", state.accountsById["acc-archived"]?.name)

        job.cancel()
    }
}
