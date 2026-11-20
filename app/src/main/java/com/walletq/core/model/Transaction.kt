package com.walletq.core.model

import kotlinx.serialization.Serializable

/**
 * Domain entity representing a business-level financial event.
 *
 * Immutable Financial Event Principles:
 * 1. [amount] represents the positive magnitude of the transaction. Sign is determined by [type].
 * 2. Transfers do not count as income or expense; they reallocate funds between accounts.
 * 3. Never mutated after posting. Corrections reference [originalTransactionId] or [reversalTransactionId].
 * 4. Strictly isolated by [ownershipType] to ensure personal or household boundary privacy.
 */
@Serializable
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val accountId: String,
    val categoryId: String? = null,
    val transactionDateEpochMs: Long,
    val description: String,
    val notes: String? = null,
    val ownershipType: OwnershipType,
    val ownerProfileId: String,
    val householdId: String? = null,
    val transferDetails: TransferDetails? = null,
    val status: TransactionStatus = TransactionStatus.POSTED,
    val originalTransactionId: String? = null,
    val reversalTransactionId: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
