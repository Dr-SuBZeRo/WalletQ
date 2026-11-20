package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walletq.core.model.OwnershipType

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: OwnershipType,
    val isDefault: Boolean,
    val createdAtEpochMs: Long
)
