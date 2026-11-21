package com.walletq.core.database.repository

import com.walletq.core.model.Category
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    fun getActiveCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>
    fun getCategoriesByProfile(profileId: String): Flow<List<Category>>
    fun getCategoriesByHousehold(householdId: String): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun createCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun archiveCategory(id: String): Result<Unit>
    suspend fun unarchiveCategory(id: String): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
}
