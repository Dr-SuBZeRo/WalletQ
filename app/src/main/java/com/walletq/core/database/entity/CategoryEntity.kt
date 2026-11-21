package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletq.core.model.TransactionType

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentCategoryId"]),
        Index(value = ["profileId"]),
        Index(value = ["householdId"]),
        Index(value = ["transactionType"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val transactionType: TransactionType,
    val iconIdentifier: String,
    val colorIdentifier: String,
    val parentCategoryId: String? = null,
    val profileId: String? = null,
    val householdId: String? = null,
    val isSystemCategory: Boolean = false,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long
)
