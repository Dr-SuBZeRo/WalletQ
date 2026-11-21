package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.walletq.core.database.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<LedgerEntryEntity>)

    @Query("SELECT * FROM ledger_entries WHERE accountId = :accountId ORDER BY timestampEpochMs ASC")
    fun observeAccountLedger(accountId: String): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE accountId = :accountId ORDER BY timestampEpochMs ASC")
    suspend fun getEntriesForAccount(accountId: String): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entries WHERE transactionId = :transactionId ORDER BY timestampEpochMs ASC")
    suspend fun getEntriesForTransaction(transactionId: String): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entries WHERE transactionId = :transactionId ORDER BY timestampEpochMs ASC")
    fun observeEntriesForTransaction(transactionId: String): Flow<List<LedgerEntryEntity>>

    @Query("SELECT COALESCE(SUM(signedMinorUnits), 0) FROM ledger_entries WHERE accountId = :accountId")
    suspend fun calculateAccountBalanceDelta(accountId: String): Long

    @Query("SELECT COALESCE(SUM(signedMinorUnits), 0) FROM ledger_entries WHERE accountId = :accountId AND timestampEpochMs <= :timestampEpochMs")
    suspend fun calculateBalanceDeltaAtTimestamp(accountId: String, timestampEpochMs: Long): Long

    @Query("SELECT COALESCE(SUM(signedMinorUnits), 0) FROM ledger_entries WHERE accountId = :accountId")
    fun observeAccountBalanceDelta(accountId: String): Flow<Long>

    @Query("SELECT COUNT(*) FROM ledger_entries")
    suspend fun getCount(): Int
}
