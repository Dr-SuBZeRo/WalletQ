package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletq.core.model.LedgerEntryType

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["currencyCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["accountId"]),
        Index(value = ["currencyCode"]),
        Index(value = ["timestampEpochMs"]),
        Index(value = ["accountId", "timestampEpochMs"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String,
    val accountId: String,
    val signedMinorUnits: Long,
    val currencyCode: String,
    val entryType: LedgerEntryType,
    val timestampEpochMs: Long
)
