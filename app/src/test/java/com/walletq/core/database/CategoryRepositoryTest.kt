package com.walletq.core.database

import com.walletq.core.database.dao.CategoryDao
import com.walletq.core.database.dao.TransactionDao
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.database.entity.TransactionEntity
import com.walletq.core.database.repository.CategoryRepositoryImpl
import com.walletq.core.model.Category
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableMapOf<String, CategoryEntity>()

        override suspend fun insert(category: CategoryEntity): Long {
            categories[category.id] = category
            return 1L
        }

        override suspend fun insertAll(categories: List<CategoryEntity>) {
            categories.forEach { insert(it) }
        }

        override suspend fun update(category: CategoryEntity) {
            categories[category.id] = category
        }

        override suspend fun delete(category: CategoryEntity) {
            categories.remove(category.id)
        }

        override suspend fun getById(id: String): CategoryEntity? = categories[id]

        override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(categories.values.toList())

        override fun observeActive(): Flow<List<CategoryEntity>> =
            flowOf(categories.values.filter { !it.isArchived })

        override fun observeByType(type: TransactionType): Flow<List<CategoryEntity>> =
            flowOf(categories.values.filter { it.transactionType == type && !it.isArchived })

        override fun observeSystemCategories(): Flow<List<CategoryEntity>> =
            flowOf(categories.values.filter { it.isSystemCategory && !it.isArchived })

        override fun observeByProfile(profileId: String): Flow<List<CategoryEntity>> =
            flowOf(categories.values.filter { it.profileId == profileId && !it.isArchived })

        override fun observeByHousehold(householdId: String): Flow<List<CategoryEntity>> =
            flowOf(categories.values.filter { it.householdId == householdId && !it.isArchived })

        override suspend fun setArchived(id: String, isArchived: Boolean) {
            val existing = categories[id] ?: return
            categories[id] = existing.copy(isArchived = isArchived)
        }

        override suspend fun getCount(): Int = categories.size
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()

        override suspend fun countTransactionsUsingCategory(categoryId: String): Int =
            transactions.count { it.categoryId == categoryId }

        override suspend fun insert(transaction: TransactionEntity) { transactions.add(transaction) }
        override suspend fun insertAll(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
        override suspend fun update(transaction: TransactionEntity) {}
        override suspend fun getById(id: String): TransactionEntity? = null
        override fun getTransactionFlow(id: String): Flow<TransactionEntity?> = flowOf(null)
        override fun observeAll(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByAccount(accountId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByProfile(profileId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByHousehold(householdId: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByDateRange(startDateEpochMs: Long, endDateEpochMs: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun updateReversalStatus(id: String, status: TransactionStatus, reversalTransactionId: String, updatedAtEpochMs: Long) {}
        override suspend fun getCount(): Int = 0
    }

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setup() {
        categoryDao = FakeCategoryDao()
        transactionDao = FakeTransactionDao()
        repository = CategoryRepositoryImpl(categoryDao, transactionDao)
    }

    @Test
    fun createCategory_validatesOwnershipBoundaries() = runTest {
        // System category with profileId -> fails
        val invalidSystem = Category(
            id = "cat-inv-sys",
            name = "Invalid System",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "tag",
            colorIdentifier = "#000",
            profileId = "profile-1", // Invalid
            isSystemCategory = true
        )
        assertTrue(repository.createCategory(invalidSystem).isFailure)

        // Custom category with neither profile nor household -> fails
        val orphanCustom = Category(
            id = "cat-orphan",
            name = "Orphan Custom",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "tag",
            colorIdentifier = "#000",
            profileId = null,
            householdId = null,
            isSystemCategory = false
        )
        assertTrue(repository.createCategory(orphanCustom).isFailure)

        // Valid Personal Category
        val personalCat = Category(
            id = "cat-personal-gym",
            name = "Gym & Fitness",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "fitness_center",
            colorIdentifier = "#4CAF50",
            profileId = "profile-1",
            householdId = null,
            isSystemCategory = false
        )
        assertTrue(repository.createCategory(personalCat).isSuccess)

        // Valid Household Category
        val householdCat = Category(
            id = "cat-hh-groceries",
            name = "Bulk Grocery",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "local_grocery_store",
            colorIdentifier = "#2196F3",
            profileId = null,
            householdId = "hh-1",
            isSystemCategory = false
        )
        assertTrue(repository.createCategory(householdCat).isSuccess)
    }

    @Test
    fun archiveAndUnarchiveCategory_togglesArchivedFlag() = runTest {
        val cat = Category(
            id = "cat-sub",
            name = "Subscriptions",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "subscriptions",
            colorIdentifier = "#9C27B0",
            profileId = "profile-1",
            isSystemCategory = false
        )
        repository.createCategory(cat)

        repository.archiveCategory("cat-sub")
        val archived = repository.getCategoryById("cat-sub")
        assertNotNull(archived)
        assertTrue(archived!!.isArchived)

        // Active query should exclude archived categories
        val activeList = repository.getActiveCategories().first()
        assertFalse(activeList.any { it.id == "cat-sub" })

        // Unarchive
        repository.unarchiveCategory("cat-sub")
        val unarchived = repository.getCategoryById("cat-sub")
        assertFalse(unarchived!!.isArchived)
    }

    @Test
    fun deleteCategory_preventsDeletingSystemCategories() = runTest {
        val systemCat = Category(
            id = "cat-sys-salary",
            name = "Salary",
            transactionType = TransactionType.INCOME,
            iconIdentifier = "work",
            colorIdentifier = "#4CAF50",
            isSystemCategory = true
        )
        categoryDao.insert(CategoryEntity(
            id = "cat-sys-salary",
            name = "Salary",
            transactionType = TransactionType.INCOME,
            iconIdentifier = "work",
            colorIdentifier = "#4CAF50",
            isSystemCategory = true,
            isArchived = false,
            createdAtEpochMs = 1000L
        ))

        val result = repository.deleteCategory("cat-sys-salary")
        assertTrue(result.isFailure)
        assertNotNull(categoryDao.getById("cat-sys-salary"))
    }

    @Test
    fun deleteCategory_preventsDeletingReferencedCategories_butAllowsUnreferenced() = runTest {
        categoryDao.insert(CategoryEntity(
            id = "cat-in-use",
            name = "Active Category",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "tag",
            colorIdentifier = "#000",
            profileId = "profile-1",
            isSystemCategory = false,
            isArchived = false,
            createdAtEpochMs = 1000L
        ))
        categoryDao.insert(CategoryEntity(
            id = "cat-unused",
            name = "Unused Category",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "tag",
            colorIdentifier = "#000",
            profileId = "profile-1",
            isSystemCategory = false,
            isArchived = false,
            createdAtEpochMs = 1000L
        ))

        // Link transaction to cat-in-use
        transactionDao.insert(
            TransactionEntity(
                id = "tx-1",
                transactionType = TransactionType.EXPENSE,
                transactionStatus = TransactionStatus.POSTED,
                amountMinorUnits = 1000L,
                currencyCode = "INR",
                sourceAccountId = "acc-1",
                categoryId = "cat-in-use",
                description = "Expense with category",
                transactionDateEpochMs = 1000L,
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 1000L
            )
        )

        // Attempting to delete referenced category fails
        val deleteInUse = repository.deleteCategory("cat-in-use")
        assertTrue(deleteInUse.isFailure)
        assertNotNull(categoryDao.getById("cat-in-use"))

        // Deleting unused custom category succeeds
        val deleteUnused = repository.deleteCategory("cat-unused")
        assertTrue(deleteUnused.isSuccess)
        assertNull(categoryDao.getById("cat-unused"))
    }
}
