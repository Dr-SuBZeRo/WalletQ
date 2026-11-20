package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.walletq.core.model.HouseholdRole

@Entity(
    tableName = "household_members",
    primaryKeys = ["householdId", "profileId"],
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["householdId"])
    ]
)
data class HouseholdMemberEntity(
    val householdId: String,
    val profileId: String,
    val role: HouseholdRole,
    val joinedAtEpochMs: Long
)
