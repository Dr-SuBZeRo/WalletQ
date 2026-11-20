package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exchange_rates",
    foreignKeys = [
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["baseCurrencyCode"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["targetCurrencyCode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["baseCurrencyCode"]),
        Index(value = ["targetCurrencyCode"])
    ]
)
data class ExchangeRateEntity(
    @PrimaryKey
    val id: String,
    val baseCurrencyCode: String,
    val targetCurrencyCode: String,
    val rate: String,
    val timestampEpochMs: Long
)
