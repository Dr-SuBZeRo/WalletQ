package com.walletq.core.di

import android.content.Context
import androidx.room.Room
import com.walletq.core.database.DatabaseCallback
import com.walletq.core.database.WalletQDatabase
import com.walletq.core.database.dao.AccountDao
import com.walletq.core.database.dao.CurrencyDao
import com.walletq.core.database.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWalletQDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<WalletQDatabase>
    ): WalletQDatabase {
        return Room.databaseBuilder(
            context,
            WalletQDatabase::class.java,
            WalletQDatabase.DATABASE_NAME
        )
            .addCallback(DatabaseCallback(databaseProvider))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProfileDao(database: WalletQDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideAccountDao(database: WalletQDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCurrencyDao(database: WalletQDatabase): CurrencyDao = database.currencyDao()
}
