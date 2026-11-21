package com.walletq.core.database.repository

import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.mapper.toDomain
import com.walletq.core.database.mapper.toEntity
import com.walletq.core.model.Category
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveCategories(): Flow<List<Category>> {
        return categoryDao.observeActive().map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.observeByType(type).map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByProfile(profileId: String): Flow<List<Category>> {
        return categoryDao.observeByProfile(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByHousehold(householdId: String): Flow<List<Category>> {
        return categoryDao.observeByHousehold(householdId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getById(id)?.toDomain()
    }

    override suspend fun createCategory(category: Category): Result<Unit> = runCatching {
        require(category.name.isNotBlank()) { "Category name cannot be blank." }
        if (category.isSystemCategory) {
            require(category.profileId == null && category.householdId == null) {
                "System category must not specify a profileId or householdId."
            }
        } else {
            require((category.profileId != null && category.householdId == null) ||
                    (category.profileId == null && category.householdId != null)) {
                "Custom category must belong to either a profile or a household, not both or neither."
            }
        }
        categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        require(category.name.isNotBlank()) { "Category name cannot be blank." }
        val existing = categoryDao.getById(category.id)
            ?: throw IllegalArgumentException("Category '${category.id}' does not exist.")
        if (existing.isSystemCategory) {
            require(category.isSystemCategory) { "Cannot change system status of a system category." }
        }
        categoryDao.update(category.toEntity())
    }

    override suspend fun archiveCategory(id: String): Result<Unit> = runCatching {
        val existing = categoryDao.getById(id)
            ?: throw IllegalArgumentException("Category '$id' does not exist.")
        categoryDao.setArchived(existing.id, true)
    }

    override suspend fun unarchiveCategory(id: String): Result<Unit> = runCatching {
        val existing = categoryDao.getById(id)
            ?: throw IllegalArgumentException("Category '$id' does not exist.")
        categoryDao.setArchived(existing.id, false)
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        val existing = categoryDao.getById(id)
            ?: throw IllegalArgumentException("Category '$id' does not exist.")
        if (existing.isSystemCategory) {
            throw IllegalStateException("System categories cannot be deleted.")
        }
        val usageCount = transactionDao.countTransactionsUsingCategory(id)
        if (usageCount > 0) {
            throw IllegalStateException(
                "Cannot delete category '${existing.name}' because it is referenced by $usageCount posted transaction(s). Archive it instead."
            )
        }
        categoryDao.delete(existing)
    }
}
