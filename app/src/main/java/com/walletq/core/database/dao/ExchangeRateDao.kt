package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.walletq.core.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRateEntity>)

    @Query("""
        SELECT * FROM exchange_rates 
        WHERE baseCurrencyCode = :baseCode AND targetCurrencyCode = :targetCode
        ORDER BY timestampEpochMs DESC 
        LIMIT 1
    """)
    suspend fun getLatestRate(baseCode: String, targetCode: String): ExchangeRateEntity?

    @Query("""
        SELECT * FROM exchange_rates 
        WHERE baseCurrencyCode = :baseCode AND targetCurrencyCode = :targetCode
        ORDER BY timestampEpochMs DESC
    """)
    fun getRateHistoryFlow(baseCode: String, targetCode: String): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates ORDER BY timestampEpochMs DESC")
    fun getAllRatesFlow(): Flow<List<ExchangeRateEntity>>
}
