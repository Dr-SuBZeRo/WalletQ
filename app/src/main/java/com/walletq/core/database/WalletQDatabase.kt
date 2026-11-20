package com.walletq.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        AccountEntity::class,
        CurrencyEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WalletQDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun currencyDao(): CurrencyDao

    companion object {
        const val DATABASE_NAME = "walletq.db"
    }
}
