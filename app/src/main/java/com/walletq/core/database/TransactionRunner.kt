package com.walletq.core.database

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for executing database operations within an atomic transaction.
 * Allows decoupling repository logic from Room openHelper instances in unit tests
 * while running [RoomDatabase.withTransaction] in production.
 */
interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: WalletQDatabase
) : TransactionRunner {
    override suspend operator fun <T> invoke(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }
}
