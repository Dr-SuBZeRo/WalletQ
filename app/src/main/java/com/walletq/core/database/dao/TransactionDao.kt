package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionFlow(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun observeByProfile(profileId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE householdId = :householdId ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun observeByHousehold(householdId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionDateEpochMs BETWEEN :startDateEpochMs AND :endDateEpochMs ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun observeByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionType = :type ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun observeByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :query || '%' OR (notes IS NOT NULL AND notes LIKE '%' || :query || '%') ORDER BY transactionDateEpochMs DESC, createdAtEpochMs DESC")
    fun search(query: String): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET transactionStatus = :status, reversalTransactionId = :reversalTransactionId, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateReversalStatus(
        id: String,
        status: TransactionStatus,
        reversalTransactionId: String,
        updatedAtEpochMs: Long
    )

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsUsingCategory(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int
}
