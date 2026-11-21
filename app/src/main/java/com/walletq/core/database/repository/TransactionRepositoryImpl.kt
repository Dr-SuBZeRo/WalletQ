package com.walletq.core.database.repository

import com.walletq.core.database.TransactionRunner
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.mapper.toEntity
import com.walletq.core.model.LedgerEngine
import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.ReplacementResult
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val transactionDao: TransactionDao,
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val householdDao: HouseholdDao
) : TransactionRepository {

    override suspend fun recordTransaction(transaction: Transaction): Result<Transaction> = runCatching {
        transactionRunner {
            // 1. Fetch and validate source account
            val sourceEntity = accountDao.getById(transaction.accountId)
                ?: throw IllegalArgumentException("Source account '${transaction.accountId}' does not exist.")
            val sourceAccount = sourceEntity.toDomain()
            val accountsMap = mutableMapOf(sourceAccount.id to sourceAccount)

            // 2. Fetch and validate destination account for transfers
            if (transaction.transferDetails != null) {
                val destEntity = accountDao.getById(transaction.transferDetails.destinationAccountId)
                    ?: throw IllegalArgumentException("Destination account '${transaction.transferDetails.destinationAccountId}' does not exist.")
                val destAccount = destEntity.toDomain()
                accountsMap[destAccount.id] = destAccount
            }

            // 3. Validate category if referenced
            if (transaction.categoryId != null) {
                val category = categoryDao.getById(transaction.categoryId)
                    ?: throw IllegalArgumentException("Category '${transaction.categoryId}' does not exist.")
                require(!category.isArchived) {
                    "Cannot record transaction with archived category '${category.name}'."
                }
            }

            // 4. Fetch household members for ownership validation if applicable
            val householdMembers = if (transaction.householdId != null) {
                householdDao.getMembersForHousehold(transaction.householdId).map { it.toDomain() }
            } else null

            // 5. Pure Kotlin LedgerEngine validation
            LedgerEngine.validateTransaction(transaction, accountsMap, householdMembers)

            // 6. Generate paired double-entry ledger records
            val ledgerEntries = LedgerEngine.generateLedgerEntries(transaction)

            // 7. Atomic persistence
            transactionDao.insert(transaction.toEntity())
            ledgerDao.insertAll(ledgerEntries.map { it.toEntity() })

            // 8. Re-calculate and project account balances
            val sourceDelta = ledgerDao.calculateAccountBalanceDelta(sourceAccount.id)
            val sourceNewBalance = Math.addExact(sourceAccount.initialBalanceMinorUnits, sourceDelta)
            accountDao.updateCurrentBalance(sourceAccount.id, sourceNewBalance)

            if (transaction.transferDetails != null) {
                val destId = transaction.transferDetails.destinationAccountId
                val destAccount = accountsMap[destId]!!
                val destDelta = ledgerDao.calculateAccountBalanceDelta(destId)
                val destNewBalance = Math.addExact(destAccount.initialBalanceMinorUnits, destDelta)
                accountDao.updateCurrentBalance(destId, destNewBalance)
            }

            transaction
        }
    }

    override suspend fun reverseTransaction(transactionId: String, reason: String?): Result<Transaction> = runCatching {
        transactionRunner {
            val originalEntity = transactionDao.getById(transactionId)
                ?: throw IllegalArgumentException("Transaction '$transactionId' does not exist.")
            val originalTransaction = originalEntity.toDomain()

            if (originalTransaction.status == TransactionStatus.REVERSED) {
                throw IllegalStateException("Transaction '$transactionId' is already reversed.")
            }

            val now = System.currentTimeMillis()
            val reversalTxId = UUID.randomUUID().toString()
            val reversalResult = LedgerEngine.createReversal(
                original = originalTransaction,
                reversalTransactionId = reversalTxId,
                timestampEpochMs = now,
                reason = reason ?: "User correction"
            )

            // Update original transaction status to REVERSED with reversal reference
            transactionDao.updateReversalStatus(
                id = originalTransaction.id,
                status = TransactionStatus.REVERSED,
                reversalTransactionId = reversalResult.reversalTransaction.id,
                updatedAtEpochMs = now
            )

            // Insert reversal transaction and compensating ledger entries
            transactionDao.insert(reversalResult.reversalTransaction.toEntity())
            val compensatingEntities = reversalResult.compensatingLedgerEntries.map { it.toEntity() }
            ledgerDao.insertAll(compensatingEntities)

            // Re-project source balance
            val sourceEntity = accountDao.getById(originalTransaction.accountId)
                ?: throw IllegalStateException("Account '${originalTransaction.accountId}' not found during reversal.")
            val sourceDelta = ledgerDao.calculateAccountBalanceDelta(sourceEntity.id)
            val sourceNewBalance = Math.addExact(sourceEntity.initialBalanceMinorUnits, sourceDelta)
            accountDao.updateCurrentBalance(sourceEntity.id, sourceNewBalance)

            // Re-project destination balance if transfer
            if (originalTransaction.transferDetails != null) {
                val destId = originalTransaction.transferDetails.destinationAccountId
                val destEntity = accountDao.getById(destId)
                    ?: throw IllegalStateException("Account '$destId' not found during reversal.")
                val destDelta = ledgerDao.calculateAccountBalanceDelta(destId)
                val destNewBalance = Math.addExact(destEntity.initialBalanceMinorUnits, destDelta)
                accountDao.updateCurrentBalance(destId, destNewBalance)
            }

            reversalResult.reversalTransaction
        }
    }

    override suspend fun replaceTransaction(
        originalTransactionId: String,
        replacement: Transaction,
        reason: String?
    ): Result<ReplacementResult> = runCatching {
        transactionRunner {
            val originalEntity = transactionDao.getById(originalTransactionId)
                ?: throw IllegalArgumentException("Transaction '$originalTransactionId' does not exist.")
            val originalTransaction = originalEntity.toDomain()

            if (originalTransaction.status == TransactionStatus.REVERSED) {
                throw IllegalStateException("Cannot replace transaction '$originalTransactionId' because it is already reversed.")
            }

            // Fetch replacement accounts
            val sourceEntity = accountDao.getById(replacement.accountId)
                ?: throw IllegalArgumentException("Replacement source account '${replacement.accountId}' does not exist.")
            val sourceAccount = sourceEntity.toDomain()
            val accountsMap = mutableMapOf(sourceAccount.id to sourceAccount)

            if (replacement.transferDetails != null) {
                val destEntity = accountDao.getById(replacement.transferDetails.destinationAccountId)
                    ?: throw IllegalArgumentException("Replacement destination account '${replacement.transferDetails.destinationAccountId}' does not exist.")
                val destAccount = destEntity.toDomain()
                accountsMap[destAccount.id] = destAccount
            }

            if (replacement.categoryId != null) {
                val category = categoryDao.getById(replacement.categoryId)
                    ?: throw IllegalArgumentException("Replacement category '${replacement.categoryId}' does not exist.")
                require(!category.isArchived) { "Cannot replace with archived category '${category.name}'." }
            }

            val householdMembers = if (replacement.householdId != null) {
                householdDao.getMembersForHousehold(replacement.householdId).map { it.toDomain() }
            } else null

            // Validate replacement
            LedgerEngine.validateTransaction(replacement, accountsMap, householdMembers)

            val now = System.currentTimeMillis()
            val reversalTxId = UUID.randomUUID().toString()
            val result = LedgerEngine.createReplacement(
                original = originalTransaction,
                replacementTransaction = replacement,
                reversalTransactionId = reversalTxId,
                timestampEpochMs = now,
                reason = reason ?: "User correction"
            )

            // Update original transaction status
            transactionDao.updateReversalStatus(
                id = originalTransaction.id,
                status = TransactionStatus.REVERSED,
                reversalTransactionId = result.reversalTransaction.id,
                updatedAtEpochMs = now
            )

            // Persist reversal
            transactionDao.insert(result.reversalTransaction.toEntity())
            val revEntities = result.reversalLedgerEntries.map { it.toEntity() }
            ledgerDao.insertAll(revEntities)

            // Persist replacement
            transactionDao.insert(result.replacementTransaction.toEntity())
            val repEntities = result.replacementLedgerEntries.map { it.toEntity() }
            ledgerDao.insertAll(repEntities)

            // Update balances for all affected accounts
            val affectedAccountIds = mutableSetOf(
                originalTransaction.accountId,
                replacement.accountId
            )
            originalTransaction.transferDetails?.let { affectedAccountIds.add(it.destinationAccountId) }
            replacement.transferDetails?.let { affectedAccountIds.add(it.destinationAccountId) }

            for (accId in affectedAccountIds) {
                val acc = accountDao.getById(accId)
                if (acc != null) {
                    val delta = ledgerDao.calculateAccountBalanceDelta(accId)
                    val newBal = Math.addExact(acc.initialBalanceMinorUnits, delta)
                    accountDao.updateCurrentBalance(accId, newBal)
                }
            }

            result
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    override fun getTransactionFlow(id: String): Flow<Transaction?> {
        return transactionDao.getTransactionFlow(id).map { it?.toDomain() }
    }

    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.observeByAccount(accountId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByProfile(profileId: String): Flow<List<Transaction>> {
        return transactionDao.observeByProfile(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByHousehold(householdId: String): Flow<List<Transaction>> {
        return transactionDao.observeByHousehold(householdId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<Transaction>> {
        return transactionDao.observeByDateRange(startDateEpochMs, endDateEpochMs).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.observeByType(type).map { list -> list.map { it.toDomain() } }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.search(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getLedgerEntriesForAccount(accountId: String): Flow<List<LedgerEntry>> {
        return ledgerDao.observeAccountLedger(accountId).map { list -> list.map { it.toDomain() } }
    }

    override fun getLedgerEntriesForTransaction(transactionId: String): Flow<List<LedgerEntry>> {
        return ledgerDao.observeEntriesForTransaction(transactionId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun calculateAccountBalance(accountId: String): Long {
        val account = accountDao.getById(accountId)
            ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
        val delta = ledgerDao.calculateAccountBalanceDelta(accountId)
        return Math.addExact(account.initialBalanceMinorUnits, delta)
    }

    override suspend fun calculateBalanceAtTimestamp(accountId: String, timestampEpochMs: Long): Long {
        val account = accountDao.getById(accountId)
            ?: throw IllegalArgumentException("Account '$accountId' does not exist.")
        val delta = ledgerDao.calculateBalanceDeltaAtTimestamp(accountId, timestampEpochMs)
        return Math.addExact(account.initialBalanceMinorUnits, delta)
    }
}
