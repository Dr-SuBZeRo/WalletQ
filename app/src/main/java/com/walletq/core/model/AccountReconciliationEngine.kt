package com.walletq.core.model

/**
 * Pure Kotlin read-only reconciliation engine.
 *
 * Compares an account's opening balance and ledger entries against its stored balance projection
 * and audits transaction-to-ledger invariants without ever mutating or silently correcting state.
 */
object AccountReconciliationEngine {

    fun reconcile(
        account: Account,
        transactionsForAccount: List<Transaction>,
        ledgerEntriesForAccount: List<LedgerEntry>
    ): AccountReconciliationReport {
        val discrepancies = mutableListOf<ReconciliationDiscrepancy>()

        // 1. Check duplicate ledger entry IDs
        val duplicateEntryIds = ledgerEntriesForAccount
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
        for (dupId in duplicateEntryIds) {
            discrepancies.add(
                ReconciliationDiscrepancy(
                    type = DiscrepancyType.DUPLICATE_LEDGER_ENTRY,
                    message = "Duplicate ledger entry ID '$dupId' detected."
                )
            )
        }

        // 2. Check currency mismatches & compute ledger delta safely (only matching currency entries)
        var ledgerDelta = 0L
        for (entry in ledgerEntriesForAccount) {
            if (entry.currency.code != account.currency.code) {
                discrepancies.add(
                    ReconciliationDiscrepancy(
                        type = DiscrepancyType.CURRENCY_MISMATCH,
                        message = "Ledger entry '${entry.id}' has currency '${entry.currency.code}', expected '${account.currency.code}'.",
                        transactionId = entry.transactionId
                    )
                )
            } else {
                ledgerDelta = Math.addExact(ledgerDelta, entry.signedMinorUnits)
            }
        }

        // 3. Group entries and transactions by transaction ID
        val entriesByTxId = ledgerEntriesForAccount.groupBy { it.transactionId }
        val transactionsById = transactionsForAccount.associateBy { it.id }

        // Detect orphan ledger entries (entries with no matching transaction)
        for ((txId, entries) in entriesByTxId) {
            if (!transactionsById.containsKey(txId)) {
                discrepancies.add(
                    ReconciliationDiscrepancy(
                        type = DiscrepancyType.INCONSISTENT_LEDGER_ENTRY_COUNT,
                        message = "Found ${entries.size} ledger entry/entries referencing missing transaction '$txId'.",
                        transactionId = txId
                    )
                )
            }
        }

        // 4. Verify each transaction touching this account has exactly 1 matching ledger entry
        for (tx in transactionsForAccount) {
            val touchesAsSource = tx.accountId == account.id
            val touchesAsDest = tx.type == TransactionType.TRANSFER &&
                tx.transferDetails?.destinationAccountId == account.id

            if (!touchesAsSource && !touchesAsDest) continue

            val txEntries = entriesByTxId[tx.id].orEmpty()
            when {
                txEntries.isEmpty() -> {
                    discrepancies.add(
                        ReconciliationDiscrepancy(
                            type = DiscrepancyType.MISSING_LEDGER_ENTRY,
                            message = "Transaction '${tx.description}' (${tx.id}) is missing its ledger entry.",
                            transactionId = tx.id
                        )
                    )
                }
                txEntries.size > 1 -> {
                    discrepancies.add(
                        ReconciliationDiscrepancy(
                            type = DiscrepancyType.DUPLICATE_LEDGER_ENTRY,
                            message = "Transaction '${tx.description}' (${tx.id}) has ${txEntries.size} ledger entries for this account (expected 1).",
                            transactionId = tx.id
                        )
                    )
                }
                else -> {
                    val entry = txEntries.single()
                    val isReversalTx = tx.originalTransactionId != null &&
                        transactionsById[tx.originalTransactionId]?.reversalTransactionId == tx.id
                    val expectedSigned = expectedSignedMinorUnits(tx, account.id, isReversalTx)
                    if (expectedSigned != null && entry.signedMinorUnits != expectedSigned) {
                        discrepancies.add(
                            ReconciliationDiscrepancy(
                                type = DiscrepancyType.INCONSISTENT_LEDGER_ENTRY_COUNT,
                                message = "Ledger entry amount (${entry.signedMinorUnits}) does not match transaction (${expectedSigned}) for '${tx.description}'.",
                                transactionId = tx.id
                            )
                        )
                    }
                }
            }

            // 5. Verify reversed transactions have complete compensating entries
            if (tx.status == TransactionStatus.REVERSED) {
                val revId = tx.reversalTransactionId
                if (revId == null) {
                    discrepancies.add(
                        ReconciliationDiscrepancy(
                            type = DiscrepancyType.INCOMPLETE_REVERSAL,
                            message = "Reversed transaction '${tx.description}' (${tx.id}) has no reversalTransactionId.",
                            transactionId = tx.id
                        )
                    )
                } else {
                    val revTx = transactionsById[revId]
                    val revEntries = entriesByTxId[revId].orEmpty()
                    val origEntries = entriesByTxId[tx.id].orEmpty()
                    if (revTx == null || revEntries.size != 1 || origEntries.size != 1) {
                        discrepancies.add(
                            ReconciliationDiscrepancy(
                                type = DiscrepancyType.INCOMPLETE_REVERSAL,
                                message = "Reversed transaction '${tx.description}' (${tx.id}) is missing its compensating reversal record or ledger entry.",
                                transactionId = tx.id
                            )
                        )
                    } else if (origEntries.single().signedMinorUnits + revEntries.single().signedMinorUnits != 0L) {
                        discrepancies.add(
                            ReconciliationDiscrepancy(
                                type = DiscrepancyType.INCOMPLETE_REVERSAL,
                                message = "Compensating reversal entry for '${tx.description}' (${tx.id}) does not zero out the original entry.",
                                transactionId = tx.id
                            )
                        )
                    }
                }
            }
        }

        // 6. Compare Expected vs Stored balance projection
        val expectedBalance = Math.addExact(account.initialBalanceMinorUnits, ledgerDelta)
        val storedBalance = account.currentBalanceMinorUnits
        val difference = Math.subtractExact(expectedBalance, storedBalance)

        if (difference != 0L) {
            val expectedFmt = Money(expectedBalance, account.currency).format()
            val storedFmt = Money(storedBalance, account.currency).format()
            discrepancies.add(
                ReconciliationDiscrepancy(
                    type = DiscrepancyType.PROJECTION_MISMATCH,
                    message = "Stored balance ($storedFmt) differs from ledger-derived expected balance ($expectedFmt)."
                )
            )
        }

        val status = if (discrepancies.isEmpty()) {
            ReconciliationStatus.BALANCED
        } else {
            ReconciliationStatus.DISCREPANCY_FOUND
        }

        return AccountReconciliationReport(
            accountId = account.id,
            accountName = account.name,
            currency = account.currency,
            openingBalanceMinorUnits = account.initialBalanceMinorUnits,
            ledgerDeltaMinorUnits = ledgerDelta,
            expectedBalanceMinorUnits = expectedBalance,
            storedBalanceMinorUnits = storedBalance,
            differenceMinorUnits = difference,
            status = status,
            discrepancies = discrepancies
        )
    }

    private fun expectedSignedMinorUnits(
        tx: Transaction,
        accountId: String,
        isReversalTx: Boolean
    ): Long? {
        val base = when (tx.type) {
            TransactionType.INCOME -> if (tx.accountId == accountId) tx.amount.minorUnits else null
            TransactionType.EXPENSE -> if (tx.accountId == accountId) -tx.amount.minorUnits else null
            TransactionType.TRANSFER -> when (accountId) {
                tx.accountId -> -tx.amount.minorUnits
                tx.transferDetails?.destinationAccountId -> tx.transferDetails.destinationAmount.minorUnits
                else -> null
            }
        } ?: return null
        return if (isReversalTx) Math.negateExact(base) else base
    }
}
