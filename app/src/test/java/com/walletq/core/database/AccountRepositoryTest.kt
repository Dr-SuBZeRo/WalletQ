package com.walletq.core.database

import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.LedgerEntryEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.database.repository.AccountRepositoryImpl
import com.walletq.core.model.Account
import com.walletq.core.model.AccountType
import com.walletq.core.model.Currency
import com.walletq.core.model.HouseholdRole
import com.walletq.core.model.LedgerEntryType
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountRepositoryTest {

    private class FakeAccountDao : AccountDao {
        val accounts = MutableStateFlow<Map<String, AccountEntity>>(emptyMap())

        override suspend fun insert(account: AccountEntity) {
            accounts.value = accounts.value + (account.id to account)
        }

        override suspend fun update(account: AccountEntity) {
            accounts.value = accounts.value + (account.id to account)
        }

        override suspend fun delete(account: AccountEntity) {
            accounts.value = accounts.value - account.id
        }

        override suspend fun getById(id: String): AccountEntity? = accounts.value[id]

        override fun getAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> =
            accounts.map { map -> map.values.filter { it.profileId == profileId && !it.isArchived } }

        override fun getAccountsByHouseholdFlow(householdId: String): Flow<List<AccountEntity>> =
            accounts.map { map -> map.values.filter { it.householdId == householdId && !it.isArchived } }

        override fun getPersonalAccountsByProfileFlow(profileId: String): Flow<List<AccountEntity>> =
            accounts.map { map -> map.values.filter { it.profileId == profileId && it.ownershipType == OwnershipType.PERSONAL && !it.isArchived } }

        override fun getAllActiveAccountsFlow(): Flow<List<AccountEntity>> =
            accounts.map { map -> map.values.filter { !it.isArchived } }

        override fun observeAllAccountsFlow(): Flow<List<AccountEntity>> =
            accounts.map { map -> map.values.toList() }

        override fun observeAccountByIdFlow(id: String): Flow<AccountEntity?> =
            accounts.map { map -> map[id] }

        override suspend fun findByNameInPersonalScope(profileId: String, name: String): AccountEntity? =
            accounts.value.values.find {
                it.profileId == profileId &&
                    it.ownershipType == OwnershipType.PERSONAL &&
                    it.name.trim().equals(name.trim(), ignoreCase = true)
            }

        override suspend fun findByNameInHouseholdScope(householdId: String, name: String): AccountEntity? =
            accounts.value.values.find {
                it.householdId == householdId &&
                    it.ownershipType == OwnershipType.HOUSEHOLD &&
                    it.name.trim().equals(name.trim(), ignoreCase = true)
            }

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

    private class FakeProfileDao : ProfileDao {
        val profiles = mutableMapOf(
            "profile-1" to ProfileEntity("profile-1", "Personal", OwnershipType.PERSONAL, true, 1000L),
            "profile-2" to ProfileEntity("profile-2", "Outsider", OwnershipType.PERSONAL, false, 1000L)
        )
        override suspend fun insert(profile: ProfileEntity) { profiles[profile.id] = profile }
        override suspend fun update(profile: ProfileEntity) { profiles[profile.id] = profile }
        override suspend fun delete(profile: ProfileEntity) { profiles.remove(profile.id) }
        override suspend fun getById(id: String): ProfileEntity? = profiles[id]
        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flowOf(profiles.values.toList())
        override fun getDefaultProfileFlow(): Flow<ProfileEntity?> = flowOf(profiles["profile-1"])
        override suspend fun getCount(): Int = profiles.size
    }

    private class FakeHouseholdDao : HouseholdDao {
        val households = mutableMapOf(
            "hh-1" to HouseholdEntity("hh-1", "Family", "Shared household", 1000L)
        )
        val members = mutableListOf(
            HouseholdMemberEntity("hh-1", "profile-1", HouseholdRole.OWNER, 1000L)
        )
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

    private class FakeCurrencyDao : CurrencyDao {
        val currencies = Currency.SUPPORTED_CURRENCIES.associate {
            it.code to CurrencyEntity(it.code, it.displayName, it.symbol, it.decimalPlaces, it.code == "INR")
        }
        override suspend fun insertAll(currencies: List<CurrencyEntity>) {}
        override suspend fun insert(currency: CurrencyEntity) {}
        override fun getAllCurrenciesFlow(): Flow<List<CurrencyEntity>> = flowOf(currencies.values.toList())
        override suspend fun getByCode(code: String): CurrencyEntity? = currencies[code]
        override fun getDefaultReportingCurrencyFlow(): Flow<CurrencyEntity?> = flowOf(currencies["INR"])
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        override suspend fun insert(transaction: TransactionEntity) { transactions.add(transaction) }
        override suspend fun insertAll(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
        override suspend fun update(transaction: TransactionEntity) {}
        override suspend fun getById(id: String): TransactionEntity? = transactions.find { it.id == id }
        override fun getTransactionFlow(id: String): Flow<TransactionEntity?> = flowOf(null)
        override fun observeAll(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun observeByAccount(accountId: String): Flow<List<TransactionEntity>> = flowOf(transactions.filter { it.sourceAccountId == accountId || it.destinationAccountId == accountId })
        override fun observeByProfile(profileId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByHousehold(householdId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun updateReversalStatus(id: String, status: TransactionStatus, reversalTransactionId: String, updatedAtEpochMs: Long) {}
        override suspend fun countTransactionsUsingCategory(categoryId: String): Int = 0
        override suspend fun countTransactionsForAccount(accountId: String): Int =
            transactions.count { it.sourceAccountId == accountId || it.destinationAccountId == accountId }
        override suspend fun getCount(): Int = transactions.size
    }

    private class FakeLedgerDao : LedgerDao {
        val entries = mutableListOf<LedgerEntryEntity>()
        override suspend fun insert(entry: LedgerEntryEntity) { entries.add(entry) }
        override suspend fun insertAll(entries: List<LedgerEntryEntity>) { this.entries.addAll(entries) }
        override fun observeAccountLedger(accountId: String): Flow<List<LedgerEntryEntity>> = flowOf(entries.filter { it.accountId == accountId })
        override suspend fun getEntriesForAccount(accountId: String): List<LedgerEntryEntity> = entries.filter { it.accountId == accountId }
        override suspend fun getEntriesForTransaction(transactionId: String): List<LedgerEntryEntity> = entries.filter { it.transactionId == transactionId }
        override fun observeEntriesForTransaction(transactionId: String): Flow<List<LedgerEntryEntity>> = flowOf(emptyList())
        override suspend fun calculateAccountBalanceDelta(accountId: String): Long =
            entries.filter { it.accountId == accountId }.sumOf { it.signedMinorUnits }
        override suspend fun calculateBalanceDeltaAtTimestamp(accountId: String, timestampEpochMs: Long): Long =
            entries.filter { it.accountId == accountId && it.timestampEpochMs <= timestampEpochMs }.sumOf { it.signedMinorUnits }
        override fun observeAccountBalanceDelta(accountId: String): Flow<Long> = flowOf(0L)
        override suspend fun getCount(): Int = entries.size
    }

    private lateinit var accountDao: FakeAccountDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var ledgerDao: FakeLedgerDao
    private lateinit var repository: AccountRepositoryImpl

    @Before
    fun setup() {
        accountDao = FakeAccountDao()
        transactionDao = FakeTransactionDao()
        ledgerDao = FakeLedgerDao()
        val runner = object : TransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T = block()
        }
        repository = AccountRepositoryImpl(
            transactionRunner = runner,
            accountDao = accountDao,
            profileDao = FakeProfileDao(),
            householdDao = FakeHouseholdDao(),
            currencyDao = FakeCurrencyDao(),
            transactionDao = transactionDao,
            ledgerDao = ledgerDao
        )
    }

    @Test
    fun createAccount_personalAndHousehold_succeedsWithOpeningBalanceAndDate() = runTest {
        val personal = Account(
            id = "acc-1",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "  Primary Bank  ",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 250000L, // ₹ 2,500.00
            currentBalanceMinorUnits = 250000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L,
            openingBalanceDateEpochMs = 1699000000000L,
            description = "Salary account"
        )
        val res1 = repository.createAccount(personal)
        assertTrue(res1.isSuccess)
        val saved1 = res1.getOrThrow()
        assertEquals("Primary Bank", saved1.name)
        assertEquals(250000L, saved1.initialBalanceMinorUnits)
        assertEquals(250000L, saved1.currentBalanceMinorUnits)
        assertEquals(1699000000000L, saved1.openingBalanceDateEpochMs)

        val household = Account(
            id = "acc-2",
            profileId = "profile-1",
            householdId = "hh-1",
            ownershipType = OwnershipType.HOUSEHOLD,
            name = "Primary Bank", // Same name allowed in household scope
            type = AccountType.SAVINGS,
            currency = Currency.USD,
            initialBalanceMinorUnits = 10000L,
            currentBalanceMinorUnits = 10000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        assertTrue(repository.createAccount(household).isSuccess)
    }

    @Test
    fun createAccount_invalidHouseholdMembership_isRejected() = runTest {
        val invalidHouseholdAccount = Account(
            id = "acc-invalid-hh",
            profileId = "profile-2", // Not a member of hh-1
            householdId = "hh-1",
            ownershipType = OwnershipType.HOUSEHOLD,
            name = "Unauthorized Joint",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 0L,
            currentBalanceMinorUnits = 0L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        val result = repository.createAccount(invalidHouseholdAccount)
        assertTrue(result.isFailure)
        assertEquals(0, accountDao.getCount())
    }

    @Test
    fun createAccount_duplicateNameInSameScope_isRejected() = runTest {
        val acc1 = Account(
            id = "acc-dup-1",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "Cash Wallet",
            type = AccountType.CASH,
            currency = Currency.INR,
            initialBalanceMinorUnits = 5000L,
            currentBalanceMinorUnits = 5000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        assertTrue(repository.createAccount(acc1).isSuccess)

        val acc2 = acc1.copy(id = "acc-dup-2", name = "  cash wallet ")
        assertTrue(repository.createAccount(acc2).isFailure)
    }

    @Test
    fun updateAccountDetails_preservesOpeningBalanceDateAndFinancialHistory() = runTest {
        val acc = Account(
            id = "acc-edit",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "Checking",
            type = AccountType.BANK,
            currency = Currency.USD,
            initialBalanceMinorUnits = 100000L,
            currentBalanceMinorUnits = 100000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L,
            openingBalanceDateEpochMs = 1690000000000L
        )
        repository.createAccount(acc)

        val updated = repository.updateAccountDetails(
            accountId = "acc-edit",
            name = "Main Checking",
            type = AccountType.SAVINGS,
            description = "Updated description",
            iconIdentifier = "SAVINGS",
            colorIdentifier = "TEAL"
        ).getOrThrow()

        assertEquals("Main Checking", updated.name)
        assertEquals(AccountType.SAVINGS, updated.type)
        assertEquals("Updated description", updated.description)
        assertEquals(100000L, updated.initialBalanceMinorUnits)
        assertEquals(100000L, updated.currentBalanceMinorUnits)
        assertEquals(1690000000000L, updated.openingBalanceDateEpochMs)
    }

    @Test
    fun adjustBalance_recordsAuditableLedgerTransactionWithoutOverwritingInitialBalance() = runTest {
        val acc = Account(
            id = "acc-adj",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "Cash",
            type = AccountType.CASH,
            currency = Currency.INR,
            initialBalanceMinorUnits = 50000L, // ₹ 500.00
            currentBalanceMinorUnits = 50000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        repository.createAccount(acc)

        val adjusted = repository.adjustBalance(
            accountId = "acc-adj",
            targetBalanceMinorUnits = 75000L, // ₹ 750.00 (+250.00)
            reason = "Counted cash"
        ).getOrThrow()

        assertEquals(75000L, adjusted.currentBalanceMinorUnits)
        // Initial opening balance remains untouched for auditability
        assertEquals(50000L, adjusted.initialBalanceMinorUnits)
        assertEquals(1, transactionDao.transactions.size)
        assertEquals(TransactionType.INCOME, transactionDao.transactions.first().transactionType)
        assertEquals(25000L, transactionDao.transactions.first().amountMinorUnits)
        assertEquals(1, ledgerDao.entries.size)
        assertEquals(75000L, repository.calculateLedgerBalance("acc-adj"))
    }

    @Test
    fun archiveAccount_andDeleteGuard_preserveHistoricalTransactions() = runTest {
        val acc = Account(
            id = "acc-arch",
            profileId = "profile-1",
            ownershipType = OwnershipType.PERSONAL,
            name = "Old Bank",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 10000L,
            currentBalanceMinorUnits = 10000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        repository.createAccount(acc)

        // Simulate a historical ledger entry and balance update
        ledgerDao.insert(
            LedgerEntryEntity(
                id = "le-1",
                transactionId = "tx-1",
                accountId = "acc-arch",
                signedMinorUnits = -2000L,
                currencyCode = "INR",
                entryType = LedgerEntryType.DEBIT,
                timestampEpochMs = 1700000005000L
            )
        )
        transactionDao.insert(
            TransactionEntity(
                id = "tx-1",
                transactionType = TransactionType.EXPENSE,
                transactionStatus = TransactionStatus.POSTED,
                amountMinorUnits = 2000L,
                currencyCode = "INR",
                sourceAccountId = "acc-arch",
                profileId = "profile-1",
                description = "Coffee",
                transactionDateEpochMs = 1700000005000L,
                createdAtEpochMs = 1700000005000L,
                updatedAtEpochMs = 1700000005000L
            )
        )

        // Inconsistent balance must prevent archiving
        assertTrue(repository.archiveAccount("acc-arch").isFailure)

        // Align projected balance with ledger (10000 - 2000 = 8000)
        accountDao.updateCurrentBalance("acc-arch", 8000L)
        assertTrue(repository.archiveAccount("acc-arch").isSuccess)
        assertTrue(repository.getAccountById("acc-arch")?.isArchived == true)

        // Physical deletion must fail when financial history exists
        assertTrue(repository.deleteAccount("acc-arch").isFailure)
        assertNotNull(repository.getAccountById("acc-arch"))

        // Unarchive restores active state
        assertTrue(repository.unarchiveAccount("acc-arch").isSuccess)
        assertFalse(repository.getAccountById("acc-arch")?.isArchived ?: true)
    }
}
