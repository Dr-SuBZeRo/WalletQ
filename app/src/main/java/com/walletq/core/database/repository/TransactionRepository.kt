package com.walletq.core.database.repository

import com.walletq.core.model.LedgerEntry
import com.walletq.core.model.ReplacementResult
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun recordTransaction(transaction: Transaction): Result<Transaction>
    suspend fun reverseTransaction(transactionId: String, reason: String? = null): Result<Transaction>
    suspend fun replaceTransaction(
        originalTransactionId: String,
        replacement: Transaction,
        reason: String? = null
    ): Result<ReplacementResult>

    suspend fun getTransactionById(id: String): Transaction?
    fun getTransactionFlow(id: String): Flow<Transaction?>
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    fun getTransactionsByProfile(profileId: String): Flow<List<Transaction>>
    fun getTransactionsByHousehold(householdId: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<Transaction>>
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getLedgerEntriesForAccount(accountId: String): Flow<List<LedgerEntry>>
    fun getLedgerEntriesForTransaction(transactionId: String): Flow<List<LedgerEntry>>
    suspend fun calculateAccountBalance(accountId: String): Long
    suspend fun calculateBalanceAtTimestamp(accountId: String, timestampEpochMs: Long): Long
}
