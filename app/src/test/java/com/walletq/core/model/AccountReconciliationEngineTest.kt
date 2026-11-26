package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountReconciliationEngineTest {

    private fun sampleAccount(
        initialMinor: Long = 10000L,
        currentMinor: Long = 10000L
    ) = Account(
        id = "acc-1",
        profileId = "profile-1",
        ownershipType = OwnershipType.PERSONAL,
        name = "Checking",
        type = AccountType.BANK,
        currency = Currency.INR,
        initialBalanceMinorUnits = initialMinor,
        currentBalanceMinorUnits = currentMinor,
        isArchived = false,
        createdAtEpochMs = 1700000000000L
    )

    @Test
    fun reconcile_balancedAccountWithIncomeExpenseAndReversal_returnsBalanced() {
        val tx1 = Transaction(
            id = "tx-1",
            type = TransactionType.INCOME,
            amount = Money(5000L, Currency.INR),
            accountId = "acc-1",
            transactionDateEpochMs = 1700000001000L,
            description = "Salary",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000001000L,
            updatedAtEpochMs = 1700000001000L
        )
        val tx2 = Transaction(
            id = "tx-2",
            type = TransactionType.EXPENSE,
            amount = Money(2000L, Currency.INR),
            accountId = "acc-1",
            transactionDateEpochMs = 1700000002000L,
            description = "Coffee",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            status = TransactionStatus.REVERSED,
            reversalTransactionId = "tx-2-rev",
            createdAtEpochMs = 1700000002000L,
            updatedAtEpochMs = 1700000003000L
        )
        val tx2Rev = tx2.copy(
            id = "tx-2-rev",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.POSTED,
            originalTransactionId = "tx-2",
            reversalTransactionId = null,
            description = "Reversal: Coffee"
        )
        val entries = listOf(
            LedgerEntry("le-1", "tx-1", "acc-1", 5000L, Currency.INR, LedgerEntryType.CREDIT, 1700000001000L),
            LedgerEntry("le-2", "tx-2", "acc-1", -2000L, Currency.INR, LedgerEntryType.DEBIT, 1700000002000L),
            LedgerEntry("le-3", "tx-2-rev", "acc-1", 2000L, Currency.INR, LedgerEntryType.CREDIT, 1700000003000L)
        )

        val account = sampleAccount(initialMinor = 10000L, currentMinor = 15000L)
        val report = AccountReconciliationEngine.reconcile(account, listOf(tx1, tx2, tx2Rev), entries)

        assertEquals(ReconciliationStatus.BALANCED, report.status)
        assertEquals(15000L, report.expectedBalanceMinorUnits)
        assertEquals(15000L, report.storedBalanceMinorUnits)
        assertEquals(0L, report.differenceMinorUnits)
        assertTrue(report.discrepancies.isEmpty())
    }

    @Test
    fun reconcile_detectsProjectionMismatchWithoutMutatingAccount() {
        val account = sampleAccount(initialMinor = 10000L, currentMinor = 12000L) // Stale stored balance
        val report = AccountReconciliationEngine.reconcile(account, emptyList(), emptyList())

        assertEquals(ReconciliationStatus.DISCREPANCY_FOUND, report.status)
        assertEquals(10000L, report.expectedBalanceMinorUnits)
        assertEquals(12000L, report.storedBalanceMinorUnits)
        assertEquals(-2000L, report.differenceMinorUnits)
        assertTrue(report.discrepancies.any { it.type == DiscrepancyType.PROJECTION_MISMATCH })
    }

    @Test
    fun reconcile_detectsMissingDuplicateCurrencyAndIncompleteReversal() {
        val txMissing = Transaction(
            id = "tx-missing",
            type = TransactionType.EXPENSE,
            amount = Money(1000L, Currency.INR),
            accountId = "acc-1",
            transactionDateEpochMs = 1700000001000L,
            description = "Missing entry",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000001000L,
            updatedAtEpochMs = 1700000001000L
        )
        val txDup = txMissing.copy(id = "tx-dup", description = "Duplicate entries")
        val txBadRev = txMissing.copy(
            id = "tx-bad-rev",
            status = TransactionStatus.REVERSED,
            reversalTransactionId = null,
            description = "Incomplete reversal"
        )

        val entries = listOf(
            LedgerEntry("le-dup-1", "tx-dup", "acc-1", -1000L, Currency.INR, LedgerEntryType.DEBIT, 1700000001000L),
            LedgerEntry("le-dup-2", "tx-dup", "acc-1", -1000L, Currency.INR, LedgerEntryType.DEBIT, 1700000001000L),
            LedgerEntry("le-bad-rev", "tx-bad-rev", "acc-1", -1000L, Currency.INR, LedgerEntryType.DEBIT, 1700000001000L),
            LedgerEntry("le-cur", "tx-orphan", "acc-1", 500L, Currency.USD, LedgerEntryType.CREDIT, 1700000001000L)
        )

        val account = sampleAccount(initialMinor = 10000L, currentMinor = 7000L)
        val report = AccountReconciliationEngine.reconcile(
            account = account,
            transactionsForAccount = listOf(txMissing, txDup, txBadRev),
            ledgerEntriesForAccount = entries
        )

        assertEquals(ReconciliationStatus.DISCREPANCY_FOUND, report.status)
        val types = report.discrepancies.map { it.type }.toSet()
        assertTrue(types.contains(DiscrepancyType.MISSING_LEDGER_ENTRY))
        assertTrue(types.contains(DiscrepancyType.DUPLICATE_LEDGER_ENTRY))
        assertTrue(types.contains(DiscrepancyType.CURRENCY_MISMATCH))
        assertTrue(types.contains(DiscrepancyType.INCOMPLETE_REVERSAL))
        assertTrue(types.contains(DiscrepancyType.INCONSISTENT_LEDGER_ENTRY_COUNT))
    }
}
