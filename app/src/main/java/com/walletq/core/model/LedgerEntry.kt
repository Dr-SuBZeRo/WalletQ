package com.walletq.core.model

import kotlinx.serialization.Serializable

/**
 * Low-level immutable double-entry book entry.
 *
 * An atomic financial ledger record associated with a single account.
 * [signedMinorUnits] is positive for [LedgerEntryType.CREDIT] and negative for [LedgerEntryType.DEBIT].
 */
@Serializable
data class LedgerEntry(
    val id: String,
    val transactionId: String,
    val accountId: String,
    val signedMinorUnits: Long,
    val currency: Currency,
    val entryType: LedgerEntryType,
    val timestampEpochMs: Long
)
