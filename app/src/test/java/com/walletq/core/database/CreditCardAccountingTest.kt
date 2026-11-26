package com.walletq.core.database

import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.LedgerEntryEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.database.repository.AccountRepositoryImpl
import com.walletq.core.database.repository.TransactionRepositoryImpl
import com.walletq.core.model.Account
import com.walletq.core.model.AccountType
import com.walletq.core.model.Currency
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.ReconciliationStatus
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.core.model.TransferDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreditCardAccountingTest {

    private class FakeAccountDao : AccountDao {
        val accounts = MutableStateFlow<Map<String, AccountEntity>>(emptyMap())
        override suspend fun insert(account: AccountEntity) { accounts.value = accounts.value + (account.id to account) }
        override suspend fun update(account: AccountEntity) { accounts.value = accounts.value + (account.id to account) }
        override suspend fun delete(account: AccountEntity) { accounts.value = accounts.value - account.id }
        override suspend fun getById(id: String): AccountEntity? = accounts.value[id]
        override fun getAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> = flowOf(emptyList())
        override fun getAccountsByHouseholdFlow(householdId: String): Flow<List<AccountEntity>> = flowOf(emptyList())
        override fun getPersonalAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> = flowOf(emptyList())
        override fun getAllActiveAccountsFlow(): Flow<List<AccountEntity>> = accounts.map { it.values.filter { a -> !a.isArchived } }
        override fun observeAllAccountsFlow(): Flow<List<AccountEntity>> = accounts.map { it.values.toList() }
        override fun observeAccountByIdFlow(id: String): Flow<AccountEntity?> = accounts.map { it[id] }
        override suspend fun findByNameInPersonalScope(profileId: String, name: String): AccountEntity? = null
        override suspend fun findByNameInHouseholdScope(householdId: String, name: String): AccountEntity? = null
        override suspend fun getCount(): Int = accounts.value.size
        override suspend fun updateCurrentBalance(id: String, balance: Long) {
            val existing = accounts.value[id] ?: return
            accounts.value = accounts.value + (id to existing.copy(currentBalanceMinorUnits = balance))
        }
        override suspend fun setArchived(id: String, isArchived: Boolean) {
            val existing = accounts.value[id] ?: return
            accounts.value = accounts.value + (id to existing.copy(isArchived = isArchived))
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        override suspend fun insert(transaction: TransactionEntity) { transactions.add(transaction) }
        override suspend fun insertAll(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
        override suspend fun update(transaction: TransactionEntity) {}
        override suspend fun getById(id: String): TransactionEntity? = transactions.find { it.id == id }
        override fun getTransactionFlow(id: String): Flow<TransactionEntity?> = flowOf(null)
        override fun observeAll(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun observeByAccount(accountId: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.sourceAccountId == accountId || it.destinationAccountId == accountId })
        override fun observeByProfile(profileId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByHousehold(householdId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun updateReversalStatus(id: String, status: TransactionStatus, reversalTransactionId: String, updatedAtEpochMs: Long) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx >= 0) {
                transactions[idx] = transactions[idx].copy(
                    transactionStatus = status,
                    reversalTransactionId = reversalTransactionId,
                    updatedAtEpochMs = updatedAtEpochMs
                )
            }
        }
        override suspend fun countTransactionsUsingCategory(categoryId: String): Int = 0
        override suspend fun countTransactionsForAccount(accountId: String): Int =
            transactions.count { it.sourceAccountId == accountId || it.destinationAccountId == accountId }
        override suspend fun getCount(): Int = transactions.size
    }

    private class FakeLedgerDao : LedgerDao {
        val entries = mutableListOf<LedgerEntryEntity>()
        override suspend fun insert(entry: LedgerEntryEntity) { entries.add(entry) }
        override suspend fun insertAll(entries: List<LedgerEntryEntity>) { this.entries.addAll(entries) }
        override fun observeAccountLedger(accountId: String): Flow<List<LedgerEntryEntity>> =
            flowOf(entries.filter { it.accountId == accountId })
        override suspend fun getEntriesForAccount(accountId: String): List<LedgerEntryEntity> =
            entries.filter { it.accountId == accountId }
        override suspend fun getEntriesForTransaction(transactionId: String): List<LedgerEntryEntity> =
            entries.filter { it.transactionId == transactionId }
        override fun observeEntriesForTransaction(transactionId: String): Flow<List<LedgerEntryEntity>> = flowOf(emptyList())
        override suspend fun calculateAccountBalanceDelta(accountId: String): Long =
            entries.filter { it.accountId == accountId }.sumOf { it.signedMinorUnits }
        override suspend fun calculateBalanceDeltaAtTimestamp(accountId: String, timestampEpochMs: Long): Long =
            entries.filter { it.accountId == accountId && it.timestampEpochMs <= timestampEpochMs }.sumOf { it.signedMinorUnits }
        override fun observeAccountBalanceDelta(accountId: String): Flow<Long> = flowOf(0L)
        override suspend fun getCount(): Int = entries.size
    }

    private class FakeCategoryDao : CategoryDao {
        override suspend fun insert(category: CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<CategoryEntity>) {}
        override suspend fun update(category: CategoryEntity) {}
        override suspend fun delete(category: CategoryEntity) {}
        override suspend fun getById(id: String): CategoryEntity? = null
        override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeActive(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeByType(type: TransactionType): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeSystemCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeByProfile(profileId: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeByHousehold(householdId: String): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun setArchived(id: String, isArchived: Boolean) {}
        override suspend fun getCount(): Int = 0
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
        private val profile = ProfileEntity("profile-1", "Personal", OwnershipType.PERSONAL, true, 1000L)
        override suspend fun insert(profile: ProfileEntity) {}
        override suspend fun update(profile: ProfileEntity) {}
        override suspend fun delete(profile: ProfileEntity) {}
        override suspend fun getById(id: String): ProfileEntity? = if (id == "profile-1") profile else null
        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flowOf(listOf(profile))
        override fun getDefaultProfileFlow(): Flow<ProfileEntity?> = flowOf(profile)
        override suspend fun getCount(): Int = 1
    }

    private class FakeCurrencyDao : CurrencyDao {
        override suspend fun insertAll(currencies: List<CurrencyEntity>) {}
        override suspend fun insert(currency: CurrencyEntity) {}
        override fun getAllCurrenciesFlow(): Flow<List<CurrencyEntity>> = flowOf(emptyList())
        override suspend fun getByCode(code: String): CurrencyEntity? {
            val c = Currency.SUPPORTED_CURRENCIES.find { it.code == code } ?: return null
            return CurrencyEntity(c.code, c.displayName, c.symbol, c.decimalPlaces, c.code == "INR")
        }
        override fun getDefaultReportingCurrencyFlow(): Flow<CurrencyEntity?> = flowOf(null)
    }

    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl

    @Before
    fun setup() {
        val accountDao = FakeAccountDao()
        val transactionDao = FakeTransactionDao()
        val ledgerDao = FakeLedgerDao()
        val runner = object : TransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T = block()
        }
        accountRepository = AccountRepositoryImpl(
            transactionRunner = runner,
            accountDao = accountDao,
            profileDao = FakeProfileDao(),
            householdDao = FakeHouseholdDao(),
            currencyDao = FakeCurrencyDao(),
            transactionDao = transactionDao,
            ledgerDao = ledgerDao
        )
        transactionRepository = TransactionRepositoryImpl(
            transactionRunner = runner,
            transactionDao = transactionDao,
            ledgerDao = ledgerDao,
            accountDao = accountDao,
            categoryDao = FakeCategoryDao(),
            householdDao = FakeHouseholdDao()
        )
    }

    @Test
    fun creditCard_purchasePaymentRefundAndReversal_preserveLiabilityAndReconciliation() = runTest {
        val bank = Account(
            id = "acc-bank",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "Checking",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 100000L, // ₹ 1,000.00
            currentBalanceMinorUnits = 100000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        val card = Account(
            id = "acc-cc",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "Visa Card",
            type = AccountType.CREDIT_CARD,
            currency = Currency.INR,
            initialBalanceMinorUnits = -10000L, // -₹ 100.00 existing liability
            currentBalanceMinorUnits = -10000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        assertTrue(accountRepository.createAccount(bank).isSuccess)
        assertTrue(accountRepository.createAccount(card).isSuccess)

        // 1. Purchase on Credit Card: ₹ 250.00 EXPENSE -> balance becomes -₹ 350.00
        val purchase = Transaction(
            id = "tx-cc-purchase",
            type = TransactionType.EXPENSE,
            amount = Money(25000L, Currency.INR),
            accountId = "acc-cc",
            transactionDateEpochMs = 1700000001000L,
            description = "Groceries",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000001000L,
            updatedAtEpochMs = 1700000001000L
        )
        assertTrue(transactionRepository.recordTransaction(purchase).isSuccess)
        assertEquals(-35000L, transactionRepository.calculateAccountBalance("acc-cc"))

        // 2. Refund on Credit Card: ₹ 50.00 INCOME -> balance becomes -₹ 300.00
        val refund = Transaction(
            id = "tx-cc-refund",
            type = TransactionType.INCOME,
            amount = Money(5000L, Currency.INR),
            accountId = "acc-cc",
            transactionDateEpochMs = 1700000002000L,
            description = "Merchant refund",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000002000L,
            updatedAtEpochMs = 1700000002000L
        )
        assertTrue(transactionRepository.recordTransaction(refund).isSuccess)
        assertEquals(-30000L, transactionRepository.calculateAccountBalance("acc-cc"))

        // 3. Payment from Bank to Credit Card: ₹ 300.00 TRANSFER -> Bank = ₹ 700.00, Card = ₹ 0.00
        val payment = Transaction(
            id = "tx-cc-payment",
            type = TransactionType.TRANSFER,
            amount = Money(30000L, Currency.INR),
            accountId = "acc-bank",
            transactionDateEpochMs = 1700000003000L,
            description = "Credit Card Bill Payment",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            transferDetails = TransferDetails(
                destinationAccountId = "acc-cc",
                destinationAmount = Money(30000L, Currency.INR)
            ),
            createdAtEpochMs = 1700000003000L,
            updatedAtEpochMs = 1700000003000L
        )
        assertTrue(transactionRepository.recordTransaction(payment).isSuccess)
        assertEquals(70000L, transactionRepository.calculateAccountBalance("acc-bank"))
        assertEquals(0L, transactionRepository.calculateAccountBalance("acc-cc"))

        // 4. Reverse payment -> Bank = ₹ 1,000.00, Card = -₹ 300.00
        assertTrue(transactionRepository.reverseTransaction("tx-cc-payment", "Bounced").isSuccess)
        assertEquals(100000L, transactionRepository.calculateAccountBalance("acc-bank"))
        assertEquals(-30000L, transactionRepository.calculateAccountBalance("acc-cc"))

        // 5. Reconcile both accounts -> both must be BALANCED
        val bankReport = accountRepository.reconcileAccount("acc-bank").getOrThrow()
        val cardReport = accountRepository.reconcileAccount("acc-cc").getOrThrow()
        assertEquals(ReconciliationStatus.BALANCED, bankReport.status)
        assertEquals(ReconciliationStatus.BALANCED, cardReport.status)
        assertEquals(0L, bankReport.differenceMinorUnits)
        assertEquals(0L, cardReport.differenceMinorUnits)
    }
}
