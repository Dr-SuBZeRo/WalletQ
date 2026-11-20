package com.walletq.core.model

/**
 * Lifecycle state of a financial transaction.
 * In double-entry ledger bookkeeping, posted transactions are never silently deleted.
 * To correct a posted transaction, a compensating REVERSAL is recorded.
 */
enum class TransactionStatus {
    POSTED,
    REVERSED
}
