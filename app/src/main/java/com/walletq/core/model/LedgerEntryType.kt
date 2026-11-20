package com.walletq.core.model

/**
 * Accounting classification for a ledger entry.
 * CREDIT represents an inflow/increase to an asset account.
 * DEBIT represents an outflow/decrease from an asset account.
 */
enum class LedgerEntryType {
    CREDIT,
    DEBIT
}
