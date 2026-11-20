package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    val code: String,
    val displayName: String,
    val symbol: String,
    val decimalPlaces: Int,
    val isDefaultReporting: Boolean
)
