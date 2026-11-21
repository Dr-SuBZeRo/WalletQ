package com.walletq.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
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
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalTransactionId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["destinationCurrencyCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sourceAccountId"]),
        Index(value = ["destinationAccountId"]),
        Index(value = ["currencyCode"]),
        Index(value = ["profileId"]),
        Index(value = ["householdId"]),
        Index(value = ["categoryId"]),
        Index(value = ["originalTransactionId"]),
        Index(value = ["destinationCurrencyCode"]),
        Index(value = ["transactionDateEpochMs"]),
        Index(value = ["transactionType"]),
        Index(value = ["transactionStatus"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val transactionType: TransactionType,
    val transactionStatus: TransactionStatus,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val sourceAccountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val profileId: String? = null,
    val householdId: String? = null,
    val description: String,
    val notes: String? = null,
    val transactionDateEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val originalTransactionId: String? = null,
    val reversalTransactionId: String? = null,
    val exchangeRate: String? = null,
    val exchangeRateReferenceId: String? = null,
    val destinationAmountMinorUnits: Long? = null,
    val destinationCurrencyCode: String? = null
)
