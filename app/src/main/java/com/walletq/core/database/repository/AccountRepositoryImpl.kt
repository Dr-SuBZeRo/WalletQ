package com.walletq.core.database.repository

import com.walletq.core.database.TransactionRunner
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.mapper.toEntity
import com.walletq.core.model.Account
import com.walletq.core.model.AccountReconciliationEngine
import com.walletq.core.model.AccountReconciliationReport
import com.walletq.core.model.AccountType
import com.walletq.core.model.LedgerEngine
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val accountDao: AccountDao,
    private val profileDao: ProfileDao,
    private val householdDao: HouseholdDao,
    private val currencyDao: CurrencyDao,
    private val transactionDao: TransactionDao,
    private val ledgerDao: LedgerDao
) : AccountRepository {

    override fun observeActiveAccounts(): Flow<List<Account>> {
        return accountDao.getAllActiveAccountsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllAccounts(): Flow<List<Account>> {
        return accountDao.observeAllAccountsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAccountById(id: String): Flow<Account?> {
        return accountDao.observeAccountByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getAccountById(id: String): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    override suspend fun createAccount(account: Account): Result<Account> = runCatching {
        transactionRunner {
            val trimmedName = account.name.trim()
            require(trimmedName.isNotEmpty()) { "Account name cannot be blank." }

            currencyDao.getByCode(account.currency.code)
                ?: throw IllegalArgumentException("Currency '${account.currency.code}' is not supported.")

            profileDao.getById(account.profileId)
                ?: throw IllegalArgumentException("Profile '${account.profileId}' does not exist.")

            when (account.ownershipType) {
                OwnershipType.PERSONAL -> {
                    require(account.householdId == null) {
                        "Personal account must not specify a householdId."
                    }
                    val duplicate = accountDao.findByNameInPersonalScope(account.profileId, trimmedName)
                    require(duplicate == null) {
                        "A personal account named '$trimmedName' already exists."
                    }
                }
                OwnershipType.HOUSEHOLD -> {
                    val householdId = requireNotNull(account.householdId) {
                        "Household account must specify a householdId."
                    }
                    householdDao.getById(householdId)
                        ?: throw IllegalArgumentException("Household '$householdId' does not exist.")
                    val members = householdDao.getMembersForHousehold(householdId)
                    require(members.any { it.profileId == account.profileId }) {
                        "Profile '${account.profileId}' is not an authorized member of household '$householdId'."
                    }
                    val duplicate = accountDao.findByNameInHouseholdScope(householdId, trimmedName)
                    require(duplicate == null) {
                        "A household account named '$trimmedName' already exists in this household."
                    }
                }
            }

            if (account.type != AccountType.CREDIT_CARD) {
                require(account.initialBalanceMinorUnits >= 0L) {
                    "Opening balance cannot be negative for ${account.type.displayName}."
                }
            }

            val normalizedAccount = account.copy(
                name = trimmedName,
                currentBalanceMinorUnits = account.initialBalanceMinorUnits,
                isArchived = false,
                description = account.description?.trim()?.ifEmpty { null }
            )
            accountDao.insert(normalizedAccount.toEntity())
            normalizedAccount
        }
    }

    override suspend fun updateAccountDetails(
        accountId: String,
        name: String,
        type: AccountType,
        description: String?,
        iconIdentifier: String?,
        colorIdentifier: String?
    ): Result<Account> = runCatching {
        transactionRunner {
            val trimmedName = name.trim()
            require(trimmedName.isNotEmpty()) { "Account name cannot be blank." }

            val existing = accountDao.getById(accountId)
                ?: throw IllegalArgumentException("Account '$accountId' does not exist.")

            val duplicate = when (existing.ownershipType) {
                OwnershipType.PERSONAL ->
                    accountDao.findByNameInPersonalScope(existing.profileId, trimmedName)
                OwnershipType.HOUSEHOLD ->
                    accountDao.findByNameInHouseholdScope(existing.householdId!!, trimmedName)
            }
            require(duplicate == null || duplicate.id == accountId) {
                "Another account named '$trimmedName' already exists in this ownership scope."
            }

            // Preserve all financial, ownership, and opening-balance fields untouched.
            val updatedEntity = existing.copy(
                name = trimmedName,
                type = type,
                description = description?.trim()?.ifEmpty { null },
                iconIdentifier = iconIdentifier,
                colorIdentifier = colorIdentifier
            )
            accountDao.update(updatedEntity)
            updatedEntity.toDomain()
        }
    }

    override suspend fun adjustBalance(
        accountId: String,
        targetBalanceMinorUnits: Long,
        reason: String?
    ): Result<Account> = runCatching {
        transactionRunner {
            val existingEntity = accountDao.getById(accountId)
                ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
            val existing = existingEntity.toDomain()

            require(!existing.isArchived) {
                "Cannot adjust balance of archived account '${existing.name}'."
            }

            val ledgerDelta = ledgerDao.calculateAccountBalanceDelta(accountId)
            val currentLedgerBalance = Math.addExact(existing.initialBalanceMinorUnits, ledgerDelta)
            val adjustmentMinorUnits = Math.subtractExact(targetBalanceMinorUnits, currentLedgerBalance)

            if (adjustmentMinorUnits == 0L) {
                return@transactionRunner existing
            }

            val txType = if (adjustmentMinorUnits > 0L) TransactionType.INCOME else TransactionType.EXPENSE
            val absMinorUnits = if (adjustmentMinorUnits > 0L) {
                adjustmentMinorUnits
            } else {
                Math.negateExact(adjustmentMinorUnits)
            }
            val now = System.currentTimeMillis()
            val adjustmentTx = Transaction(
                id = UUID.randomUUID().toString(),
                type = txType,
                amount = Money(absMinorUnits, existing.currency),
                accountId = existing.id,
                categoryId = null,
                transactionDateEpochMs = now,
                description = "Opening balance adjustment",
                notes = reason?.trim()?.ifEmpty { null } ?: "Auditable balance correction",
                ownershipType = existing.ownershipType,
                ownerProfileId = existing.profileId,
                householdId = existing.householdId,
                transferDetails = null,
                status = TransactionStatus.POSTED,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )

            val householdMembers = if (existing.householdId != null) {
                householdDao.getMembersForHousehold(existing.householdId).map { it.toDomain() }
            } else null

            LedgerEngine.validateTransaction(
                transaction = adjustmentTx,
                accounts = mapOf(existing.id to existing),
                householdMembers = householdMembers
            )
            val entries = LedgerEngine.generateLedgerEntries(adjustmentTx)

            transactionDao.insert(adjustmentTx.toEntity())
            ledgerDao.insertAll(entries.map { it.toEntity() })

            val updatedDelta = ledgerDao.calculateAccountBalanceDelta(accountId)
            val newBalance = Math.addExact(existing.initialBalanceMinorUnits, updatedDelta)
            accountDao.updateCurrentBalance(accountId, newBalance)

            existing.copy(currentBalanceMinorUnits = newBalance)
        }
    }

    override suspend fun archiveAccount(accountId: String): Result<Unit> = runCatching {
        transactionRunner {
            val existing = accountDao.getById(accountId)
                ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
            check(!existing.isArchived) {
                "Account '${existing.name}' is already archived."
            }

            val ledgerDelta = ledgerDao.calculateAccountBalanceDelta(accountId)
            val derivedBalance = Math.addExact(existing.initialBalanceMinorUnits, ledgerDelta)
            check(derivedBalance == existing.currentBalanceMinorUnits) {
                "Cannot archive account '${existing.name}' because its balance projection is inconsistent with the ledger."
            }

            accountDao.setArchived(accountId, true)
        }
    }

    override suspend fun unarchiveAccount(accountId: String): Result<Unit> = runCatching {
        transactionRunner {
            val existing = accountDao.getById(accountId)
                ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
            check(existing.isArchived) {
                "Account '${existing.name}' is already active."
            }
            accountDao.setArchived(accountId, false)
        }
    }

    override suspend fun deleteAccount(accountId: String): Result<Unit> = runCatching {
        transactionRunner {
            val existing = accountDao.getById(accountId)
                ?: throw IllegalArgumentException("Account '$accountId' does not exist.")

            val txCount = transactionDao.countTransactionsForAccount(accountId)
            val ledgerEntries = ledgerDao.getEntriesForAccount(accountId)
            if (txCount > 0 || ledgerEntries.isNotEmpty()) {
                throw IllegalStateException(
                    "Cannot delete account '${existing.name}' because it has financial history ($txCount transaction(s)). Archive it instead."
                )
            }

            accountDao.delete(existing)
        }
    }

    override suspend fun calculateLedgerBalance(accountId: String): Long {
        val account = accountDao.getById(accountId)
            ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
        val delta = ledgerDao.calculateAccountBalanceDelta(accountId)
        return Math.addExact(account.initialBalanceMinorUnits, delta)
    }

    override suspend fun reconcileAccount(accountId: String): Result<AccountReconciliationReport> = runCatching {
        val account = accountDao.getById(accountId)?.toDomain()
            ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
        val transactions = transactionDao.observeByAccount(accountId).first().map { it.toDomain() }
        val entries = ledgerDao.getEntriesForAccount(accountId).map { it.toDomain() }
        AccountReconciliationEngine.reconcile(
            account = account,
            transactionsForAccount = transactions,
            ledgerEntriesForAccount = entries
        )
    }

    override fun observeAccountReconciliation(accountId: String): Flow<AccountReconciliationReport?> {
        return combine(
            accountDao.observeAccountByIdFlow(accountId),
            transactionDao.observeByAccount(accountId),
            ledgerDao.observeAccountLedger(accountId)
        ) { accountEntity, txEntities, ledgerEntities ->
            val account = accountEntity?.toDomain() ?: return@combine null
            AccountReconciliationEngine.reconcile(
                account = account,
                transactionsForAccount = txEntities.map { it.toDomain() },
                ledgerEntriesForAccount = ledgerEntities.map { it.toDomain() }
            )
        }
    }
}
