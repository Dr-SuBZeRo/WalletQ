package com.walletq.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.walletq.core.model.Currency
import com.walletq.core.model.HouseholdRole
import com.walletq.core.model.OwnershipType

/**
 * Room database callback ensuring SQLite foreign keys are enforced
 * and pre-populating required default entities synchronously inside the creation transaction.
 */
class DatabaseCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Strictly enforce SQLite Foreign Key constraints
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Atomic synchronous pre-population within the creation transaction
        Currency.SUPPORTED_CURRENCIES.forEach { currency ->
            val isDefault = if (currency.code == Currency.DEFAULT_REPORTING_CURRENCY.code) 1 else 0
            db.execSQL(
                "INSERT OR IGNORE INTO currencies (code, displayName, symbol, decimalPlaces, isDefaultReporting) VALUES (?, ?, ?, ?, ?)",
                arrayOf(currency.code, currency.displayName, currency.symbol, currency.decimalPlaces, isDefault)
            )
        }

        val now = System.currentTimeMillis()
        // Pre-populate default Personal profile
        db.execSQL(
            "INSERT OR IGNORE INTO profiles (id, name, type, isDefault, createdAtEpochMs) VALUES (?, ?, ?, ?, ?)",
            arrayOf("default-personal", "Personal", OwnershipType.PERSONAL.name, 1, now)
        )

        // Pre-populate default Primary Household
        db.execSQL(
            "INSERT OR IGNORE INTO households (id, name, description, createdAtEpochMs) VALUES (?, ?, ?, ?)",
            arrayOf("default-household", "Primary Household", "Default household unit", now)
        )

        // Pre-populate junction entry linking default personal profile as OWNER of default household
        db.execSQL(
            "INSERT OR IGNORE INTO household_members (householdId, profileId, role, joinedAtEpochMs) VALUES (?, ?, ?, ?)",
            arrayOf("default-household", "default-personal", HouseholdRole.OWNER.name, now)
        )
    }
}
