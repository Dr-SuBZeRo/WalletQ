package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletq.core.model.BudgetPeriod
import com.walletq.core.model.OwnershipType

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["currencyCode"],
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
        Index(value = ["currencyCode"]),
        Index(value = ["profileId"]),
        Index(value = ["householdId"]),
        Index(value = ["startDateEpochMs"]),
        Index(value = ["endDateEpochMs"]),
        Index(value = ["isArchived"])
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val period: BudgetPeriod,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val currencyCode: String,
    val allocatedAmountMinorUnits: Long,
    val ownershipType: OwnershipType,
    val profileId: String? = null,
    val householdId: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
