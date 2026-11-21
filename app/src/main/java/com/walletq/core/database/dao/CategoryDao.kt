package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.walletq.core.database.entity.CategoryEntity
import com.walletq.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE transactionType = :type AND isArchived = 0 ORDER BY name ASC")
    fun observeByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isSystemCategory = 1 AND isArchived = 0 ORDER BY name ASC")
    fun observeSystemCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    fun observeByProfile(profileId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE householdId = :householdId AND isArchived = 0 ORDER BY name ASC")
    fun observeByHousehold(householdId: String): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}
