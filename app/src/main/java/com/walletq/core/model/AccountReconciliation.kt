package com.walletq.core.model

enum class ReconciliationStatus {
    BALANCED,
    DISCREPANCY_FOUND
}

enum class DiscrepancyType {
    PROJECTION_MISMATCH,
    MISSING_LEDGER_ENTRY,
    DUPLICATE_LEDGER_ENTRY,
    CURRENCY_MISMATCH,
    INCONSISTENT_LEDGER_ENTRY_COUNT,
    INCOMPLETE_REVERSAL
}

data class ReconciliationDiscrepancy(
    val type: DiscrepancyType,
    val message: String,
    val transactionId: String? = null
)

data class AccountReconciliationReport(
    val accountId: String,
    val accountName: String,
    val currency: Currency,
    val openingBalanceMinorUnits: Long,
    val ledgerDeltaMinorUnits: Long,
    val expectedBalanceMinorUnits: Long,
    val storedBalanceMinorUnits: Long,
    val differenceMinorUnits: Long,
    val status: ReconciliationStatus,
    val discrepancies: List<ReconciliationDiscrepancy>
)
