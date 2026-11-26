package com.walletq.core.database.repository

import com.walletq.core.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAllBudgets(): Flow<List<Budget>>
    fun observeAccessibleBudgets(profileId: String): Flow<List<Budget>>
    fun observeBudgetById(budgetId: String): Flow<Budget?>
    suspend fun getBudgetById(budgetId: String, requesterProfileId: String): Result<Budget>
    suspend fun createBudget(budget: Budget, requesterProfileId: String): Result<Budget>
    suspend fun updateBudget(budget: Budget, requesterProfileId: String): Result<Budget>
    suspend fun archiveBudget(budgetId: String, requesterProfileId: String): Result<Unit>
    suspend fun unarchiveBudget(budgetId: String, requesterProfileId: String): Result<Unit>
}
