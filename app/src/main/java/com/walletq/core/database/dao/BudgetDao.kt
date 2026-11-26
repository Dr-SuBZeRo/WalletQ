package com.walletq.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.walletq.core.database.entity.BudgetCategoryAllocationEntity
import com.walletq.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

data class BudgetWithAllocations(
    @Embedded val budget: BudgetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "budgetId"
    )
    val allocations: List<BudgetCategoryAllocationEntity>
)

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllocations(allocations: List<BudgetCategoryAllocationEntity>)

    @Query("DELETE FROM budget_category_allocations WHERE budgetId = :budgetId")
    suspend fun deleteAllocationsForBudget(budgetId: String)

    @Transaction
    @Query("SELECT * FROM budgets ORDER BY isArchived ASC, startDateEpochMs DESC, name ASC")
    fun observeAllBudgetsWithAllocations(): Flow<List<BudgetWithAllocations>>

    @Transaction
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetWithAllocationsById(id: String): BudgetWithAllocations?

    @Transaction
    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeBudgetWithAllocationsById(id: String): Flow<BudgetWithAllocations?>

    @Query("UPDATE budgets SET isArchived = :isArchived, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun setArchived(id: String, isArchived: Boolean, updatedAtEpochMs: Long)

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun getCount(): Int
}
