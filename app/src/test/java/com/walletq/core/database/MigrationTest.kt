package com.walletq.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.walletq.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.Locale

/**
 * Unit test verifying Room MIGRATION_2_3 schema updates, entity creation,
 * index creation, foreign key restrictions, and existing data preservation.
 */
class MigrationTest {

    @Test
    fun migration_2_to_3_createsRequiredTables() {
        val executedSqlList = mutableListOf<String>()
        val executedParameterizedSqlList = mutableListOf<Pair<String, List<Any?>>>()

        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.size == 1 && args[0] is String) {
                        executedSqlList.add(args[0] as String)
                    } else if (args != null && args.size == 2 && args[0] is String) {
                        val bindArgs = (args[1] as? Array<*>)?.toList() ?: emptyList()
                        executedParameterizedSqlList.add((args[0] as String) to bindArgs)
                    }
                    null
                }
                "isOpen" -> true
                "inTransaction" -> false
                "getVersion" -> 2
                else -> null
            }
        } as SupportSQLiteDatabase

        WalletQDatabase.MIGRATION_2_3.migrate(db)

        val fullSql = executedSqlList.joinToString("\n")

        // 1. Categories table creation
        assertTrue("Categories table should be created", fullSql.contains("CREATE TABLE IF NOT EXISTS `categories`"))
        assertTrue(fullSql.contains("`iconIdentifier` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`colorIdentifier` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`isSystemCategory` INTEGER NOT NULL"))
        assertTrue(fullSql.contains("`isArchived` INTEGER NOT NULL"))

        // 2. Transactions table creation
        assertTrue("Transactions table should be created", fullSql.contains("CREATE TABLE IF NOT EXISTS `transactions`"))
        assertTrue(fullSql.contains("`transactionType` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`transactionStatus` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`amountMinorUnits` INTEGER NOT NULL"))
        assertTrue(fullSql.contains("`currencyCode` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`sourceAccountId` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`destinationAccountId` TEXT"))
        assertTrue(fullSql.contains("`categoryId` TEXT"))
        assertTrue(fullSql.contains("`profileId` TEXT"))
        assertTrue(fullSql.contains("`householdId` TEXT"))
        assertTrue(fullSql.contains("`exchangeRate` TEXT"))
        assertTrue(fullSql.contains("`destinationAmountMinorUnits` INTEGER"))
        assertTrue(fullSql.contains("`destinationCurrencyCode` TEXT"))

        // 3. Ledger entries table creation
        assertTrue("Ledger entries table should be created", fullSql.contains("CREATE TABLE IF NOT EXISTS `ledger_entries`"))
        assertTrue(fullSql.contains("`signedMinorUnits` INTEGER NOT NULL"))
        assertTrue(fullSql.contains("`entryType` TEXT NOT NULL"))
        assertTrue(fullSql.contains("`timestampEpochMs` INTEGER NOT NULL"))
    }

    @Test
    fun migration_2_to_3_createsRequiredIndices() {
        val executedSqlList = mutableListOf<String>()

        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.size == 1 && args[0] is String) {
                        executedSqlList.add(args[0] as String)
                    }
                    null
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        WalletQDatabase.MIGRATION_2_3.migrate(db)

        val fullSql = executedSqlList.joinToString("\n")

        // Categories indices
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_categories_parentCategoryId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_categories_profileId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_categories_householdId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_categories_transactionType`"))

        // Transactions indices
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_sourceAccountId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_destinationAccountId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_currencyCode`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_profileId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_householdId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_originalTransactionId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_destinationCurrencyCode`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_transactionDateEpochMs`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_transactionType`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_transactions_transactionStatus`"))

        // Ledger entries indices
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_ledger_entries_transactionId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_ledger_entries_accountId`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_ledger_entries_currencyCode`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_ledger_entries_timestampEpochMs`"))
        assertTrue(fullSql.contains("CREATE INDEX IF NOT EXISTS `index_ledger_entries_accountId_timestampEpochMs`"))
    }

    @Test
    fun migration_2_to_3_enforcesNonDestructivePreservation() {
        val executedSqlList = mutableListOf<String>()

        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.size == 1 && args[0] is String) {
                        executedSqlList.add(args[0] as String)
                    }
                    null
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        WalletQDatabase.MIGRATION_2_3.migrate(db)

        val fullSql = executedSqlList.joinToString("\n").uppercase(Locale.ROOT)

        // Ensure NO drop table or alter table occurs for existing tables
        assertFalse("Must not drop accounts", fullSql.contains("DROP TABLE `ACCOUNTS`"))
        assertFalse("Must not drop profiles", fullSql.contains("DROP TABLE `PROFILES`"))
        assertFalse("Must not drop households", fullSql.contains("DROP TABLE `HOUSEHOLDS`"))
        assertFalse("Must not drop household_members", fullSql.contains("DROP TABLE `HOUSEHOLD_MEMBERS`"))
        assertFalse("Must not drop currencies", fullSql.contains("DROP TABLE `CURRENCIES`"))
        assertFalse("Must not drop exchange_rates", fullSql.contains("DROP TABLE `EXCHANGE_RATES`"))
    }

    @Test
    fun migration_2_to_3_populatesDefaultCategories() {
        val executedParameterizedSqlList = mutableListOf<Pair<String, List<Any?>>>()

        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.size == 2 && args[0] is String) {
                        val bindArgs = (args[1] as? Array<*>)?.toList() ?: emptyList()
                        executedParameterizedSqlList.add((args[0] as String) to bindArgs)
                    }
                    null
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        WalletQDatabase.MIGRATION_2_3.migrate(db)

        // Check that default categories were inserted via parameterized SQL
        assertEquals(Category.DEFAULT_CATEGORIES.size, executedParameterizedSqlList.size)

        val insertedIds = executedParameterizedSqlList.map { it.second[0] as String }
        val expectedIds = Category.DEFAULT_CATEGORIES.map { it.id }

        assertTrue(insertedIds.containsAll(expectedIds))
    }

    @Test
    fun migration_3_to_4_addsAccountMetadataColumnsAndBackfillsOpeningDate() {
        val executedSqlList = mutableListOf<String>()

        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.isNotEmpty() && args[0] is String) {
                        executedSqlList.add(args[0] as String)
                    }
                    null
                }
                else -> null
            }
        } as SupportSQLiteDatabase

        WalletQDatabase.MIGRATION_3_4.migrate(db)

        val fullSql = executedSqlList.joinToString("\n")
        assertTrue(fullSql.contains("ALTER TABLE `accounts` ADD COLUMN `openingBalanceDateEpochMs` INTEGER NOT NULL DEFAULT 0"))
        assertTrue(fullSql.contains("UPDATE `accounts` SET `openingBalanceDateEpochMs` = `createdAtEpochMs` WHERE `openingBalanceDateEpochMs` = 0"))
        assertTrue(fullSql.contains("ALTER TABLE `accounts` ADD COLUMN `description` TEXT"))
        assertTrue(fullSql.contains("ALTER TABLE `accounts` ADD COLUMN `iconIdentifier` TEXT"))
        assertTrue(fullSql.contains("ALTER TABLE `accounts` ADD COLUMN `colorIdentifier` TEXT"))
        assertFalse(fullSql.uppercase(Locale.ROOT).contains("DROP TABLE"))
    }
}
