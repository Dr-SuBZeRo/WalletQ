package com.walletq.core.database

import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.LedgerEntryEntity
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.database.repository.TransactionRepositoryImpl
import com.walletq.core.model.AccountType
import com.walletq.core.model.Currency
import com.walletq.core.model.HouseholdRole
import com.walletq.core.model.LedgerEntryType
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.core.model.TransferDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TransactionRepositoryTest {

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableMapOf<String, TransactionEntity>()

        override suspend fun insert(transaction: TransactionEntity) {
            if (transactions.containsKey(transaction.id)) {
                throw IllegalArgumentException("Duplicate transaction ID: ${transaction.id}")
            }
            transactions[transaction.id] = transaction
        }

        override suspend fun insertAll(transactions: List<TransactionEntity>) {
            transactions.forEach { insert(it) }
        }

        override suspend fun update(transaction: TransactionEntity) {
            transactions[transaction.id] = transaction
        }

        override suspend fun getById(id: String): TransactionEntity? = transactions[id]

        override fun getTransactionFlow(id: String): Flow<TransactionEntity?> = flowOf(transactions[id])

        override fun observeAll(): Flow<List<TransactionEntity>> = flowOf(transactions.values.toList())

        override fun observeByAccount(accountId: String): Flow<List<TransactionEntity>> = flowOf(
            transactions.values.filter { it.sourceAccountId == accountId || it.destinationAccountId == accountId }
        )

        override fun observeByProfile(profileId: String): Flow<List<TransactionEntity>> = flowOf(
            transactions.values.filter { it.profileId == profileId }
        )

        override fun observeByHousehold(householdId: String): Flow<List<TransactionEntity>> = flowOf(
            transactions.values.filter { it.householdId == householdId }
        )

        override fun observeByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<TransactionEntity>> = flowOf(
            transactions.values.filter { it.transactionDateEpochMs in startDateEpochMs..endDateEpochMs }
        )

        override fun observeByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(
            transactions.values.filter { it.transactionType == type }
        )

        override fun search(query: String): Flow<List<TransactionEntity>> = flowOf(
            transactions.values.filter { it.description.contains(query, ignoreCase = true) }
        )

        override suspend fun updateReversalStatus(
            id: String,
            status: TransactionStatus,
            reversalTransactionId: String,
            updatedAtEpochMs: Long
        ) {
            val existing = transactions[id] ?: return
            transactions[id] = existing.copy(
                transactionStatus = status,
                reversalTransactionId = reversalTransactionId,
                updatedAtEpochMs = updatedAtEpochMs
            )
        }

        override suspend fun countTransactionsUsingCategory(categoryId: String): Int =
            transactions.values.count { it.categoryId == categoryId }

        override suspend fun countTransactionsForAccount(accountId: String): Int =
            transactions.values.count { it.sourceAccountId == accountId || it.destinationAccountId == accountId }

        override suspend fun getCount(): Int = transactions.size
    }

    private class FakeLedgerDao : LedgerDao {
        val entries = mutableMapOf<String, LedgerEntryEntity>()

        override suspend fun insert(entry: LedgerEntryEntity) {
            if (entries.containsKey(entry.id)) {
                throw IllegalArgumentException("Duplicate ledger entry ID: ${entry.id}")
            }
            entries[entry.id] = entry
        }

        override suspend fun insertAll(entries: List<LedgerEntryEntity>) {
            entries.forEach { insert(it) }
        }

        override fun observeAccountLedger(accountId: String): Flow<List<LedgerEntryEntity>> = flowOf(
            entries.values.filter { it.accountId == accountId }.sortedBy { it.timestampEpochMs }
        )

        override suspend fun getEntriesForAccount(accountId: String): List<LedgerEntryEntity> =
            entries.values.filter { it.accountId == accountId }.sortedBy { it.timestampEpochMs }

        override suspend fun getEntriesForTransaction(transactionId: String): List<LedgerEntryEntity> =
            entries.values.filter { it.transactionId == transactionId }.sortedBy { it.timestampEpochMs }

        override fun observeEntriesForTransaction(transactionId: String): Flow<List<LedgerEntryEntity>> = flowOf(
            entries.values.filter { it.transactionId == transactionId }
        )

        private fun getBalanceDelta(accountId: String): Long =
            entries.values.filter { it.accountId == accountId }.sumOf { it.signedMinorUnits }

        override suspend fun calculateAccountBalanceDelta(accountId: String): Long =
            getBalanceDelta(accountId)

        override suspend fun calculateBalanceDeltaAtTimestamp(accountId: String, timestampEpochMs: Long): Long =
            entries.values.filter { it.accountId == accountId && it.timestampEpochMs <= timestampEpochMs }
                .sumOf { it.signedMinorUnits }

        override fun observeAccountBalanceDelta(accountId: String): Flow<Long> = flowOf(
            getBalanceDelta(accountId)
        )

        override suspend fun getCount(): Int = entries.size
    }

    private class FakeAccountDao : AccountDao {
        val accounts = mutableMapOf<String, AccountEntity>()

        override suspend fun insert(account: AccountEntity) { accounts[account.id] = account }
        override suspend fun update(account: AccountEntity) { accounts[account.id] = account }
        override suspend fun delete(account: AccountEntity) { accounts.remove(account.id) }
        override suspend fun getById(id: String): AccountEntity? = accounts[id]
        override fun getAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> = flowOf(accounts.values.filter { it.profileId == profileId })
        override fun getAccountsByHouseholdFlow(householdId: String): Flow<List<AccountEntity>> = flowOf(accounts.values.filter { it.householdId == householdId })
        override fun getPersonalAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> = flowOf(accounts.values.filter { it.profileId == profileId && it.ownershipType == OwnershipType.PERSONAL })
        override fun getAllActiveAccountsFlow(): Flow<List<AccountEntity>> = flowOf(accounts.values.filter { !it.isArchived })
        override fun observeAllAccountsFlow(): Flow<List<AccountEntity>> = flowOf(accounts.values.toList())
        override fun observeAccountByIdFlow(id: String): Flow<AccountEntity?> = flowOf(accounts[id])
        override suspend fun findByNameInPersonalScope(profileId: String, name: String): AccountEntity? =
            accounts.values.find { it.profileId == profileId && it.ownershipType == OwnershipType.PERSONAL && it.name.trim().equals(name.trim(), ignoreCase = true) }
        override suspend fun findByNameInHouseholdScope(householdId: String, name: String): AccountEntity? =
            accounts.values.find { it.householdId == householdId && it.ownershipType == OwnershipType.HOUSEHOLD && it.name.trim().equals(name.trim(), ignoreCase = true) }
        override suspend fun getCount(): Int = accounts.size
        override suspend fun updateCurrentBalance(id: String, balance: Long) {
            val existing = accounts[id] ?: return
            accounts[id] = existing.copy(currentBalanceMinorUnits = balance)
        }
        override suspend fun setArchived(id: String, isArchived: Boolean) {
            val existing = accounts[id] ?: return
            accounts[id] = existing.copy(isArchived = isArchived)
        }
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableMapOf<String, CategoryEntity>()

        override suspend fun insert(category: CategoryEntity): Long {
            categories[category.id] = category
            return 1L
        }
        override suspend fun insertAll(categories: List<CategoryEntity>) { categories.forEach { insert(it) } }
        override suspend fun update(category: CategoryEntity) { categories[category.id] = category }
        override suspend fun delete(category: CategoryEntity) { categories.remove(category.id) }
        override suspend fun getById(id: String): CategoryEntity? = categories[id]
        override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(categories.values.toList())
        override fun observeActive(): Flow<List<CategoryEntity>> = flowOf(categories.values.filter { !it.isArchived })
        override fun observeByType(type: TransactionType): Flow<List<CategoryEntity>> = flowOf(categories.values.filter { it.transactionType == type })
        override fun observeSystemCategories(): Flow<List<CategoryEntity>> = flowOf(categories.values.filter { it.isSystemCategory })
        override fun observeByProfile(profileId: String): Flow<List<CategoryEntity>> = flowOf(categories.values.filter { it.profileId == profileId })
        override fun observeByHousehold(householdId: String): Flow<List<CategoryEntity>> = flowOf(categories.values.filter { it.householdId == householdId })
        override suspend fun setArchived(id: String, isArchived: Boolean) {
            val existing = categories[id] ?: return
            categories[id] = existing.copy(isArchived = isArchived)
        }
        override suspend fun getCount(): Int = categories.size
    }

    private class FakeHouseholdDao : HouseholdDao {
        val households = mutableMapOf<String, HouseholdEntity>()
        val members = mutableListOf<HouseholdMemberEntity>()

        override suspend fun insert(household: HouseholdEntity) { households[household.id] = household }
        override suspend fun update(household: HouseholdEntity) { households[household.id] = household }
        override suspend fun delete(household: HouseholdEntity) { households.remove(household.id) }
        override suspend fun insertMember(member: HouseholdMemberEntity) { members.add(member) }
        override suspend fun deleteMember(householdId: String, profileId: String) {
            members.removeAll { it.householdId == householdId && it.profileId == profileId }
        }
        override suspend fun getById(id: String): HouseholdEntity? = households[id]
        override fun getAllHouseholdsFlow(): Flow<List<HouseholdEntity>> = flowOf(households.values.toList())
        override fun getMembersForHouseholdFlow(householdId: String): Flow<List<HouseholdMemberEntity>> =
            flowOf(members.filter { it.householdId == householdId })
        override suspend fun getMembersForHousehold(householdId: String): List<HouseholdMemberEntity> =
            members.filter { it.householdId == householdId }
        override fun getHouseholdsForProfileFlow(profileId: String): Flow<List<HouseholdEntity>> = flowOf(emptyList())
        override suspend fun getMemberCount(householdId: String): Int = members.count { it.householdId == householdId }
        override suspend fun getHouseholdCount(): Int = households.size
    }

    private class TestTransactionRunner(
        private val transactionDao: FakeTransactionDao,
        private val ledgerDao: FakeLedgerDao,
        private val accountDao: FakeAccountDao
    ) : TransactionRunner {
        override suspend fun <T> invoke(block: suspend () -> T): T {
            // Take state snapshot for rollback verification
            val txSnapshot = HashMap(transactionDao.transactions)
            val ledgerSnapshot = HashMap(ledgerDao.entries)
            val accountSnapshot = HashMap(accountDao.accounts)

            return try {
                block()
            } catch (e: Throwable) {
                // Roll back atomic state
                transactionDao.transactions.clear()
                transactionDao.transactions.putAll(txSnapshot)
                ledgerDao.entries.clear()
                ledgerDao.entries.putAll(ledgerSnapshot)
                accountDao.accounts.clear()
                accountDao.accounts.putAll(accountSnapshot)
                throw e
            }
        }
    }

    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var ledgerDao: FakeLedgerDao
    private lateinit var accountDao: FakeAccountDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var householdDao: FakeHouseholdDao
    private lateinit var transactionRunner: TestTransactionRunner
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setup() = runTest {
        transactionDao = FakeTransactionDao()
        ledgerDao = FakeLedgerDao()
        accountDao = FakeAccountDao()
        categoryDao = FakeCategoryDao()
        householdDao = FakeHouseholdDao()
        transactionRunner = TestTransactionRunner(transactionDao, ledgerDao, accountDao)

        repository = TransactionRepositoryImpl(
            transactionRunner = transactionRunner,
            transactionDao = transactionDao,
            ledgerDao = ledgerDao,
            accountDao = accountDao,
            categoryDao = categoryDao,
            householdDao = householdDao
        )

        // Seed personal accounts
        accountDao.insert(
            AccountEntity(
                id = "acc-personal-cash",
                profileId = "profile-1",
                householdId = null,
                ownershipType = OwnershipType.PERSONAL,
                name = "Wallet Cash",
                type = AccountType.CASH,
                currencyCode = "INR",
                initialBalanceMinorUnits = 100000L, // ₹1,000.00
                currentBalanceMinorUnits = 100000L,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )
        accountDao.insert(
            AccountEntity(
                id = "acc-personal-bank",
                profileId = "profile-1",
                householdId = null,
                ownershipType = OwnershipType.PERSONAL,
                name = "Salary Checking",
                type = AccountType.BANK,
                currencyCode = "INR",
                initialBalanceMinorUnits = 500000L, // ₹5,000.00
                currentBalanceMinorUnits = 500000L,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )
        accountDao.insert(
            AccountEntity(
                id = "acc-personal-usd",
                profileId = "profile-1",
                householdId = null,
                ownershipType = OwnershipType.PERSONAL,
                name = "Travel USD",
                type = AccountType.SAVINGS,
                currencyCode = "USD",
                initialBalanceMinorUnits = 10000L, // $100.00
                currentBalanceMinorUnits = 10000L,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )

        // Seed household accounts
        householdDao.insert(
            HouseholdEntity(
                id = "hh-1",
                name = "Verma Household",
                description = "Primary Family",
                createdAtEpochMs = 1700000000000L
            )
        )
        householdDao.insertMember(
            HouseholdMemberEntity(
                householdId = "hh-1",
                profileId = "profile-1",
                role = HouseholdRole.OWNER,
                joinedAtEpochMs = 1700000000000L
            )
        )
        accountDao.insert(
            AccountEntity(
                id = "acc-hh-joint",
                profileId = "profile-1",
                householdId = "hh-1",
                ownershipType = OwnershipType.HOUSEHOLD,
                name = "Family Joint Account",
                type = AccountType.BANK,
                currencyCode = "INR",
                initialBalanceMinorUnits = 2000000L, // ₹20,000.00
                currentBalanceMinorUnits = 2000000L,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )

        // Seed category
        categoryDao.insert(
            CategoryEntity(
                id = "cat-food",
                name = "Food & Groceries",
                transactionType = TransactionType.EXPENSE,
                iconIdentifier = "restaurant",
                colorIdentifier = "#FF5722",
                parentCategoryId = null,
                profileId = null,
                householdId = null,
                isSystemCategory = true,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )
    }

    @Test
    fun recordIncome_persistsTransactionAndCreditLedger_andUpdatesBalance() = runTest {
        val income = Transaction(
            id = "tx-inc-1",
            type = TransactionType.INCOME,
            amount = Money(250000L, Currency.INR), // ₹2,500.00
            accountId = "acc-personal-bank",
            categoryId = null,
            transactionDateEpochMs = 1700000010000L,
            description = "Consulting Gig",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000010000L,
            updatedAtEpochMs = 1700000010000L
        )

        val result = repository.recordTransaction(income)
        assertTrue(result.isSuccess)

        // Verify transaction persistence
        val persistedTx = transactionDao.getById("tx-inc-1")
        assertNotNull(persistedTx)
        assertEquals(TransactionType.INCOME, persistedTx!!.transactionType)
        assertEquals(TransactionStatus.POSTED, persistedTx.transactionStatus)

        // Verify single CREDIT entry
        val entries = ledgerDao.getEntriesForTransaction("tx-inc-1")
        assertEquals(1, entries.size)
        assertEquals(250000L, entries[0].signedMinorUnits)
        assertEquals(LedgerEntryType.CREDIT, entries[0].entryType)

        // Verify balance projection: 500000 + 250000 = 750000
        val balance = repository.calculateAccountBalance("acc-personal-bank")
        assertEquals(750000L, balance)
        assertEquals(750000L, accountDao.getById("acc-personal-bank")!!.currentBalanceMinorUnits)
    }

    @Test
    fun recordExpense_persistsTransactionAndDebitLedger_andUpdatesBalance() = runTest {
        val expense = Transaction(
            id = "tx-exp-1",
            type = TransactionType.EXPENSE,
            amount = Money(35000L, Currency.INR), // ₹350.00
            accountId = "acc-personal-cash",
            categoryId = "cat-food",
            transactionDateEpochMs = 1700000020000L,
            description = "Lunch Buffet",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000020000L,
            updatedAtEpochMs = 1700000020000L
        )

        val result = repository.recordTransaction(expense)
        assertTrue(result.isSuccess)

        val entries = ledgerDao.getEntriesForTransaction("tx-exp-1")
        assertEquals(1, entries.size)
        assertEquals(-35000L, entries[0].signedMinorUnits)
        assertEquals(LedgerEntryType.DEBIT, entries[0].entryType)

        // Balance: 100000 - 35000 = 65000
        val balance = repository.calculateAccountBalance("acc-personal-cash")
        assertEquals(65000L, balance)
    }

    @Test
    fun recordSameCurrencyTransfer_debitsSource_creditsDestination_preservesTotal() = runTest {
        val transfer = Transaction(
            id = "tx-trans-1",
            type = TransactionType.TRANSFER,
            amount = Money(50000L, Currency.INR), // Transfer ₹500 from bank to cash
            accountId = "acc-personal-bank",
            transactionDateEpochMs = 1700000030000L,
            description = "ATM Withdrawal",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            transferDetails = TransferDetails(
                destinationAccountId = "acc-personal-cash",
                destinationAmount = Money(50000L, Currency.INR),
                appliedExchangeRate = BigDecimal.ONE
            ),
            createdAtEpochMs = 1700000030000L,
            updatedAtEpochMs = 1700000030000L
        )

        val result = repository.recordTransaction(transfer)
        assertTrue(result.isSuccess)

        val entries = ledgerDao.getEntriesForTransaction("tx-trans-1")
        assertEquals(2, entries.size)

        val sourceEntry = entries.first { it.accountId == "acc-personal-bank" }
        val destEntry = entries.first { it.accountId == "acc-personal-cash" }

        assertEquals(-50000L, sourceEntry.signedMinorUnits)
        assertEquals(50000L, destEntry.signedMinorUnits)
        assertEquals(0L, sourceEntry.signedMinorUnits + destEntry.signedMinorUnits)

        // Bank: 500000 - 50000 = 450000
        assertEquals(450000L, repository.calculateAccountBalance("acc-personal-bank"))
        // Cash: 100000 + 50000 = 150000
        assertEquals(150000L, repository.calculateAccountBalance("acc-personal-cash"))
    }

    @Test
    fun recordCrossCurrencyTransfer_storesExchangeRateAndPreservesMinorUnits() = runTest {
        // Exchange $50 USD to INR @ 83.50 -> ₹4,175.00
        val crossTransfer = Transaction(
            id = "tx-trans-cross",
            type = TransactionType.TRANSFER,
            amount = Money(5000L, Currency.USD), // $50.00
            accountId = "acc-personal-usd",
            transactionDateEpochMs = 1700000040000L,
            description = "Convert USD to INR",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            transferDetails = TransferDetails(
                destinationAccountId = "acc-personal-bank",
                destinationAmount = Money(417500L, Currency.INR), // ₹4,175.00
                appliedExchangeRate = BigDecimal("83.5000"),
                exchangeRateReferenceId = "rate-usd-inr-hist"
            ),
            createdAtEpochMs = 1700000040000L,
            updatedAtEpochMs = 1700000040000L
        )

        val result = repository.recordTransaction(crossTransfer)
        assertTrue(result.isSuccess)

        val txEntity = transactionDao.getById("tx-trans-cross")!!
        assertEquals("83.5000", txEntity.exchangeRate)
        assertEquals("rate-usd-inr-hist", txEntity.exchangeRateReferenceId)
        assertEquals(417500L, txEntity.destinationAmountMinorUnits)
        assertEquals("INR", txEntity.destinationCurrencyCode)

        // USD: 10000 - 5000 = 5000 ($50.00)
        assertEquals(5000L, repository.calculateAccountBalance("acc-personal-usd"))
        // Bank INR: 500000 + 417500 = 917500 (₹9,175.00)
        assertEquals(917500L, repository.calculateAccountBalance("acc-personal-bank"))
    }

    @Test
    fun reverseTransaction_restoresBalance_andMarksOriginalReversed() = runTest {
        val expense = Transaction(
            id = "tx-to-reverse",
            type = TransactionType.EXPENSE,
            amount = Money(20000L, Currency.INR),
            accountId = "acc-personal-cash",
            transactionDateEpochMs = 1700000050000L,
            description = "Mistaken Expense",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000050000L,
            updatedAtEpochMs = 1700000050000L
        )
        repository.recordTransaction(expense)
        assertEquals(80000L, repository.calculateAccountBalance("acc-personal-cash"))

        // Reverse
        val revResult = repository.reverseTransaction("tx-to-reverse", "Wrong entry")
        assertTrue(revResult.isSuccess)

        // Original status is REVERSED
        val original = transactionDao.getById("tx-to-reverse")!!
        assertEquals(TransactionStatus.REVERSED, original.transactionStatus)
        assertNotNull(original.reversalTransactionId)

        // Balance restored to 100000
        assertEquals(100000L, repository.calculateAccountBalance("acc-personal-cash"))

        // Attempting to reverse again fails
        val secondReversal = repository.reverseTransaction("tx-to-reverse")
        assertTrue(secondReversal.isFailure)
    }

    @Test
    fun replaceTransaction_reversesOriginal_postsReplacement_andUpdatesBalances() = runTest {
        val original = Transaction(
            id = "tx-orig",
            type = TransactionType.EXPENSE,
            amount = Money(10000L, Currency.INR), // ₹100.00
            accountId = "acc-personal-cash",
            transactionDateEpochMs = 1700000060000L,
            description = "Expense typo",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000060000L,
            updatedAtEpochMs = 1700000060000L
        )
        repository.recordTransaction(original)
        assertEquals(90000L, repository.calculateAccountBalance("acc-personal-cash"))

        val replacement = original.copy(
            id = "tx-corrected",
            amount = Money(25000L, Currency.INR), // ₹250.00
            description = "Corrected expense"
        )

        val repResult = repository.replaceTransaction("tx-orig", replacement, "Amount correction")
        assertTrue(repResult.isSuccess)

        val origEntity = transactionDao.getById("tx-orig")!!
        assertEquals(TransactionStatus.REVERSED, origEntity.transactionStatus)

        val repEntity = transactionDao.getById("tx-corrected")!!
        assertEquals(TransactionStatus.POSTED, repEntity.transactionStatus)
        assertEquals("tx-orig", repEntity.originalTransactionId)

        // Balance: 100000 - 25000 = 75000
        assertEquals(75000L, repository.calculateAccountBalance("acc-personal-cash"))
    }

    @Test
    fun atomicRollback_onFailure_restoresPreTransactionState() = runTest {
        val invalidTx = Transaction(
            id = "tx-duplicate",
            type = TransactionType.EXPENSE,
            amount = Money(10000L, Currency.INR),
            accountId = "acc-personal-cash",
            transactionDateEpochMs = 1700000070000L,
            description = "Will succeed first time",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000070000L,
            updatedAtEpochMs = 1700000070000L
        )
        repository.recordTransaction(invalidTx)
        assertEquals(90000L, repository.calculateAccountBalance("acc-personal-cash"))

        // Attempting to record duplicate transaction ID
        val duplicateTx = invalidTx.copy(description = "Duplicate attempt")
        val failResult = repository.recordTransaction(duplicateTx)

        assertTrue(failResult.isFailure)
        // Verify balance was NOT mutated by failed attempt
        assertEquals(90000L, repository.calculateAccountBalance("acc-personal-cash"))
    }

    @Test
    fun personalOwnership_onHouseholdAccount_isRejected() = runTest {
        val tx = Transaction(
            id = "tx-invalid-scope",
            type = TransactionType.EXPENSE,
            amount = Money(50000L, Currency.INR),
            accountId = "acc-hh-joint", // Household account
            transactionDateEpochMs = 1700000080000L,
            description = "Personal expense on household",
            ownershipType = OwnershipType.PERSONAL, // Mismatched!
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000080000L,
            updatedAtEpochMs = 1700000080000L
        )

        val result = repository.recordTransaction(tx)
        assertTrue(result.isFailure)
    }

    @Test
    fun historicalBalanceCalculation_respectsTimestampFilter() = runTest {
        val t1 = Transaction(
            id = "tx-hist-1",
            type = TransactionType.INCOME,
            amount = Money(10000L, Currency.INR),
            accountId = "acc-personal-cash",
            transactionDateEpochMs = 1000L,
            description = "Day 1",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        val t2 = Transaction(
            id = "tx-hist-2",
            type = TransactionType.INCOME,
            amount = Money(20000L, Currency.INR),
            accountId = "acc-personal-cash",
            transactionDateEpochMs = 2000L,
            description = "Day 2",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 2000L,
            updatedAtEpochMs = 2000L
        )

        repository.recordTransaction(t1)
        repository.recordTransaction(t2)

        // Initial = 100000
        // At t=500 -> 100000
        assertEquals(100000L, repository.calculateBalanceAtTimestamp("acc-personal-cash", 500L))
        // At t=1500 -> 100000 + 10000 = 110000
        assertEquals(110000L, repository.calculateBalanceAtTimestamp("acc-personal-cash", 1500L))
        // At t=2500 -> 100000 + 10000 + 20000 = 130000
        assertEquals(130000L, repository.calculateBalanceAtTimestamp("acc-personal-cash", 2500L))
    }

    @Test
    fun mixedTransfer_unauthorizedProfileToHousehold_isRejected() = runTest {
        accountDao.insert(
            AccountEntity(
                id = "acc-other-person",
                profileId = "profile-2", // NOT a member of hh-1
                householdId = null,
                ownershipType = OwnershipType.PERSONAL,
                name = "Other Person Wallet",
                type = AccountType.CASH,
                currencyCode = "INR",
                initialBalanceMinorUnits = 100000L,
                currentBalanceMinorUnits = 100000L,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )

        val unauthorizedMixedTransfer = Transaction(
            id = "tx-unauth-mixed",
            type = TransactionType.TRANSFER,
            amount = Money(10000L, Currency.INR),
            accountId = "acc-other-person",
            transactionDateEpochMs = 1700000090000L,
            description = "Unauthorized transfer into household",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-2",
            householdId = null,
            transferDetails = TransferDetails(
                destinationAccountId = "acc-hh-joint",
                destinationAmount = Money(10000L, Currency.INR),
                appliedExchangeRate = BigDecimal.ONE
            ),
            createdAtEpochMs = 1700000090000L,
            updatedAtEpochMs = 1700000090000L
        )

        val result = repository.recordTransaction(unauthorizedMixedTransfer)
        assertTrue(result.isFailure)
        assertEquals(100000L, repository.calculateAccountBalance("acc-other-person"))
        assertEquals(2000000L, repository.calculateAccountBalance("acc-hh-joint"))
    }

    @Test
    fun recordTransaction_categoryTypeAndOwnershipMismatch_isRejected() = runTest {
        // Expense category on INCOME transaction
        val wrongTypeTx = Transaction(
            id = "tx-wrong-cat-type",
            type = TransactionType.INCOME,
            amount = Money(10000L, Currency.INR),
            accountId = "acc-personal-cash",
            categoryId = "cat-food", // EXPENSE category
            transactionDateEpochMs = 1700000095000L,
            description = "Invalid category type",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000095000L,
            updatedAtEpochMs = 1700000095000L
        )
        assertTrue(repository.recordTransaction(wrongTypeTx).isFailure)

        // Personal category owned by profile-2 on profile-1's transaction
        categoryDao.insert(
            CategoryEntity(
                id = "cat-profile-2",
                name = "Profile 2 Private",
                transactionType = TransactionType.EXPENSE,
                iconIdentifier = "spa",
                colorIdentifier = "#009688",
                profileId = "profile-2",
                householdId = null,
                isSystemCategory = false,
                isArchived = false,
                createdAtEpochMs = 1700000000000L
            )
        )
        val crossProfileCatTx = Transaction(
            id = "tx-wrong-cat-owner",
            type = TransactionType.EXPENSE,
            amount = Money(10000L, Currency.INR),
            accountId = "acc-personal-cash", // profile-1
            categoryId = "cat-profile-2",
            transactionDateEpochMs = 1700000096000L,
            description = "Cross-profile category",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000096000L,
            updatedAtEpochMs = 1700000096000L
        )
        assertTrue(repository.recordTransaction(crossProfileCatTx).isFailure)
    }

    @Test
    fun archivedAccount_blocksNewTransactions_butAllowsReversalOfHistoricalTransaction() = runTest {
        val initialTx = Transaction(
            id = "tx-before-archive",
            type = TransactionType.EXPENSE,
            amount = Money(10000L, Currency.INR),
            accountId = "acc-personal-cash",
            categoryId = "cat-food",
            transactionDateEpochMs = 1700000097000L,
            description = "Lunch before archive",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000097000L,
            updatedAtEpochMs = 1700000097000L
        )
        assertTrue(repository.recordTransaction(initialTx).isSuccess)
        assertEquals(90000L, repository.calculateAccountBalance("acc-personal-cash"))

        // Archive the account
        accountDao.setArchived("acc-personal-cash", true)

        // New transaction on archived account must fail
        val newTxOnArchived = initialTx.copy(id = "tx-after-archive", description = "Should fail")
        assertTrue(repository.recordTransaction(newTxOnArchived).isFailure)

        // Transfer to archived destination must fail
        val transferToArchived = Transaction(
            id = "tx-transfer-to-archived",
            type = TransactionType.TRANSFER,
            amount = Money(5000L, Currency.INR),
            accountId = "acc-personal-bank",
            transactionDateEpochMs = 1700000098000L,
            description = "Transfer to archived",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            transferDetails = TransferDetails(
                destinationAccountId = "acc-personal-cash",
                destinationAmount = Money(5000L, Currency.INR),
                appliedExchangeRate = BigDecimal.ONE
            ),
            createdAtEpochMs = 1700000098000L,
            updatedAtEpochMs = 1700000098000L
        )
        assertTrue(repository.recordTransaction(transferToArchived).isFailure)

        // Reversing the historical transaction must still succeed and restore balance
        val reversal = repository.reverseTransaction("tx-before-archive", "Correction after archive")
        assertTrue(reversal.isSuccess)
        assertEquals(100000L, repository.calculateAccountBalance("acc-personal-cash"))
    }
}
