package com.walletq.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.ExchangeRateDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.ExchangeRateEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        AccountEntity::class,
        CurrencyEntity::class,
        HouseholdEntity::class,
        HouseholdMemberEntity::class,
        ExchangeRateEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WalletQDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun householdDao(): HouseholdDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        const val DATABASE_NAME = "walletq.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create households table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `households` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. Create household_members junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `household_members` (
                        `householdId` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `joinedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`householdId`, `profileId`),
                        FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_members_profileId` ON `household_members` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_members_householdId` ON `household_members` (`householdId`)")

                // 3. Create exchange_rates table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exchange_rates` (
                        `id` TEXT NOT NULL,
                        `baseCurrencyCode` TEXT NOT NULL,
                        `targetCurrencyCode` TEXT NOT NULL,
                        `rate` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`baseCurrencyCode`) REFERENCES `currencies`(`code`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`targetCurrencyCode`) REFERENCES `currencies`(`code`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exchange_rates_baseCurrencyCode` ON `exchange_rates` (`baseCurrencyCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exchange_rates_targetCurrencyCode` ON `exchange_rates` (`targetCurrencyCode`)")

                // 4. Rebuild accounts table with householdId, ownershipType, and strict foreign keys
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts_new` (
                        `id` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `householdId` TEXT,
                        `ownershipType` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `initialBalanceMinorUnits` INTEGER NOT NULL,
                        `currentBalanceMinorUnits` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`currencyCode`) REFERENCES `currencies`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `accounts_new` (
                        `id`, `profileId`, `householdId`, `ownershipType`, `name`, `type`, `currencyCode`,
                        `initialBalanceMinorUnits`, `currentBalanceMinorUnits`, `isArchived`, `createdAtEpochMs`
                    )
                    SELECT 
                        `id`, `profileId`, NULL, 'PERSONAL', `name`, `type`, `currencyCode`,
                        `initialBalanceMinorUnits`, `currentBalanceMinorUnits`, `isArchived`, `createdAtEpochMs`
                    FROM `accounts`
                """.trimIndent())

                db.execSQL("DROP TABLE `accounts`")
                db.execSQL("ALTER TABLE `accounts_new` RENAME TO `accounts`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_profileId` ON `accounts` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_householdId` ON `accounts` (`householdId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_currencyCode` ON `accounts` (`currencyCode`)")
            }
        }
    }
}
