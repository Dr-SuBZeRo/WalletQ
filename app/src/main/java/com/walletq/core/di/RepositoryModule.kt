package com.walletq.core.di

import com.walletq.core.database.repository.AccountRepository
import com.walletq.core.database.repository.AccountRepositoryImpl
import com.walletq.core.database.repository.CategoryRepository
import com.walletq.core.database.repository.CategoryRepositoryImpl
import com.walletq.core.database.repository.TransactionRepository
import com.walletq.core.database.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository
}
