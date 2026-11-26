package com.walletq.core.database.repository

import com.walletq.core.model.Account
import com.walletq.core.model.AccountReconciliationReport
import com.walletq.core.model.AccountType
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeActiveAccounts(): Flow<List<Account>>
    fun observeAllAccounts(): Flow<List<Account>>
    fun observeAccountById(id: String): Flow<Account?>
    suspend fun getAccountById(id: String): Account?

    suspend fun createAccount(account: Account): Result<Account>

    suspend fun updateAccountDetails(
        accountId: String,
        name: String,
        type: AccountType,
        description: String?,
        iconIdentifier: String?,
        colorIdentifier: String?
    ): Result<Account>

    suspend fun adjustBalance(
        accountId: String,
        targetBalanceMinorUnits: Long,
        reason: String? = null
    ): Result<Account>

    suspend fun archiveAccount(accountId: String): Result<Unit>
    suspend fun unarchiveAccount(accountId: String): Result<Unit>
    suspend fun deleteAccount(accountId: String): Result<Unit>
    suspend fun calculateLedgerBalance(accountId: String): Long
    suspend fun reconcileAccount(accountId: String): Result<AccountReconciliationReport>
    fun observeAccountReconciliation(accountId: String): Flow<AccountReconciliationReport?>
}
