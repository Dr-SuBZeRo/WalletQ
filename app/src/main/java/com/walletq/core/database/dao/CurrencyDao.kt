package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.walletq.core.database.entity.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<CurrencyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: CurrencyEntity)

    @Query("SELECT * FROM currencies ORDER BY isDefaultReporting DESC, code ASC")
    fun getAllCurrenciesFlow(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE code = :code")
    suspend fun getByCode(code: String): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE isDefaultReporting = 1 LIMIT 1")
    fun getDefaultReportingCurrencyFlow(): Flow<CurrencyEntity?>
}
