package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val createdAtEpochMs: Long
)
