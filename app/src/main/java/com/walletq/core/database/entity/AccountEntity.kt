package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletq.core.model.AccountType

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalanceMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val isArchived: Boolean,
    val createdAtEpochMs: Long
)
