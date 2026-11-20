package com.walletq.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.model.Currency
import com.walletq.core.model.OwnershipType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

/**
 * Room database callback ensuring SQLite foreign keys are enforced
 * and pre-populating required currency and default profile entities.
 */
class DatabaseCallback(
    private val databaseProvider: Provider<WalletQDatabase>
) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Enforce SQLite Foreign Key constraints
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            val database = databaseProvider.get()

            // Pre-populate default ISO currencies
            val initialCurrencies = Currency.SUPPORTED_CURRENCIES.map { currency ->
                CurrencyEntity(
                    code = currency.code,
                    displayName = currency.displayName,
                    symbol = currency.symbol,
                    decimalPlaces = currency.decimalPlaces,
                    isDefaultReporting = currency.code == Currency.DEFAULT_REPORTING_CURRENCY.code
                )
            }
            database.currencyDao().insertAll(initialCurrencies)

            // Pre-populate default Personal profile
            val defaultPersonalProfile = ProfileEntity(
                id = "default-personal",
                name = "Personal",
                type = OwnershipType.PERSONAL,
                isDefault = true,
                createdAtEpochMs = System.currentTimeMillis()
            )
            database.profileDao().insert(defaultPersonalProfile)
        }
    }
}
