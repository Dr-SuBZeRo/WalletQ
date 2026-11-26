package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_category_allocations",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["budgetId"]),
        Index(value = ["categoryId"]),
        Index(value = ["budgetId", "categoryId"], unique = true)
    ]
)
data class BudgetCategoryAllocationEntity(
    @PrimaryKey
    val id: String,
    val budgetId: String,
    val categoryId: String,
    val allocatedAmountMinorUnits: Long
)
