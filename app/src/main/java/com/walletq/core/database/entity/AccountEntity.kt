package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletq.core.model.AccountType
import com.walletq.core.model.OwnershipType

@Entity(
    tableName = "accounts",
    foreignKeys = [
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
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["currencyCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["householdId"]),
        Index(value = ["currencyCode"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val householdId: String? = null,
    val ownershipType: OwnershipType = OwnershipType.PERSONAL,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalanceMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val isArchived: Boolean,
    val createdAtEpochMs: Long,
    val openingBalanceDateEpochMs: Long = createdAtEpochMs,
    val description: String? = null,
    val iconIdentifier: String? = null,
    val colorIdentifier: String? = null
)
