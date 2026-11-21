package com.walletq.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.ExchangeRateDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.entity.AccountEntity
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.database.entity.CurrencyEntity
import com.walletq.core.database.entity.ExchangeRateEntity
import com.walletq.core.database.entity.HouseholdEntity
import com.walletq.core.database.entity.HouseholdMemberEntity
import com.walletq.core.database.entity.LedgerEntryEntity
import com.walletq.core.database.entity.ProfileEntity
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.model.Category

@Database(
    entities = [
        ProfileEntity::class,
        AccountEntity::class,
        CurrencyEntity::class,
        HouseholdEntity::class,
        HouseholdMemberEntity::class,
        ExchangeRateEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        LedgerEntryEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WalletQDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun householdDao(): HouseholdDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun ledgerDao(): LedgerDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `iconIdentifier` TEXT NOT NULL,
                        `colorIdentifier` TEXT NOT NULL,
                        `parentCategoryId` TEXT,
                        `profileId` TEXT,
                        `householdId` TEXT,
                        `isSystemCategory` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`parentCategoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentCategoryId` ON `categories` (`parentCategoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_profileId` ON `categories` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_householdId` ON `categories` (`householdId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_transactionType` ON `categories` (`transactionType`)")

                // 2. Create transactions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transactions` (
                        `id` TEXT NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `transactionStatus` TEXT NOT NULL,
                        `amountMinorUnits` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `sourceAccountId` TEXT NOT NULL,
                        `destinationAccountId` TEXT,
                        `categoryId` TEXT,
                        `profileId` TEXT,
                        `householdId` TEXT,
                        `description` TEXT NOT NULL,
                        `notes` TEXT,
                        `transactionDateEpochMs` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        `originalTransactionId` TEXT,
                        `reversalTransactionId` TEXT,
                        `exchangeRate` TEXT,
                        `exchangeRateReferenceId` TEXT,
                        `destinationAmountMinorUnits` INTEGER,
                        `destinationCurrencyCode` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`destinationAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`currencyCode`) REFERENCES `currencies`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`originalTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`destinationCurrencyCode`) REFERENCES `currencies`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_sourceAccountId` ON `transactions` (`sourceAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_destinationAccountId` ON `transactions` (`destinationAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_currencyCode` ON `transactions` (`currencyCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_profileId` ON `transactions` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_householdId` ON `transactions` (`householdId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_originalTransactionId` ON `transactions` (`originalTransactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_destinationCurrencyCode` ON `transactions` (`destinationCurrencyCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transactionDateEpochMs` ON `transactions` (`transactionDateEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transactionType` ON `transactions` (`transactionType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transactionStatus` ON `transactions` (`transactionStatus`)")

                // 3. Create ledger_entries table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ledger_entries` (
                        `id` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `signedMinorUnits` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `entryType` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`currencyCode`) REFERENCES `currencies`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_transactionId` ON `ledger_entries` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_accountId` ON `ledger_entries` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_currencyCode` ON `ledger_entries` (`currencyCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_timestampEpochMs` ON `ledger_entries` (`timestampEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_accountId_timestampEpochMs` ON `ledger_entries` (`accountId`, `timestampEpochMs`)")

                // 4. Pre-populate default system categories idempotently
                val now = System.currentTimeMillis()
                Category.DEFAULT_CATEGORIES.forEach { category ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `categories` (
                            `id`, `name`, `transactionType`, `iconIdentifier`, `colorIdentifier`,
                            `parentCategoryId`, `profileId`, `householdId`, `isSystemCategory`, `isArchived`, `createdAtEpochMs`
                        ) VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, 1, 0, ?)
                        """.trimIndent(),
                        arrayOf(
                            category.id,
                            category.name,
                            category.transactionType.name,
                            category.iconIdentifier,
                            category.colorIdentifier,
                            now
                        )
                    )
                }
            }
        }
    }
}
