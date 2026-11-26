package com.walletq.core.di

import android.content.Context
import androidx.room.Room
import com.walletq.core.database.DatabaseCallback
import com.walletq.core.database.WalletQDatabase
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.ExchangeRateDao
import com.walletq.core.database.dao.HouseholdDao
import com.walletq.core.database.dao.LedgerDao
import com.walletq.core.database.dao.ProfileDao
import com.walletq.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWalletQDatabase(
        @ApplicationContext context: Context
    ): WalletQDatabase {
        return Room.databaseBuilder(
            context,
            WalletQDatabase::class.java,
            WalletQDatabase.DATABASE_NAME
        )
            .addCallback(DatabaseCallback())
            .addMigrations(
                WalletQDatabase.MIGRATION_1_2,
                WalletQDatabase.MIGRATION_2_3,
                WalletQDatabase.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    fun provideProfileDao(database: WalletQDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideAccountDao(database: WalletQDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCurrencyDao(database: WalletQDatabase): CurrencyDao = database.currencyDao()

    @Provides
    fun provideHouseholdDao(database: WalletQDatabase): HouseholdDao = database.householdDao()

    @Provides
    fun provideExchangeRateDao(database: WalletQDatabase): ExchangeRateDao = database.exchangeRateDao()

    @Provides
    fun provideCategoryDao(database: WalletQDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: WalletQDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideLedgerDao(database: WalletQDatabase): LedgerDao = database.ledgerDao()

    @Provides
    @Singleton
    fun provideTransactionRunner(database: WalletQDatabase): com.walletq.core.database.TransactionRunner =
        com.walletq.core.database.RoomTransactionRunner(database)
}
