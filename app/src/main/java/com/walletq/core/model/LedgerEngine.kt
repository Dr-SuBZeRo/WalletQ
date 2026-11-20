package com.walletq.core.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Result data carrier for a transaction reversal operation.
 */
data class ReversalResult(
    val updatedOriginalTransaction: Transaction,
    val reversalTransaction: Transaction,
    val compensatingLedgerEntries: List<LedgerEntry>
)

/**
 * Result data carrier for a transaction correction/replacement operation.
 */
data class ReplacementResult(
    val reversedOriginalTransaction: Transaction,
    val reversalTransaction: Transaction,
    val reversalLedgerEntries: List<LedgerEntry>,
    val replacementTransaction: Transaction,
    val replacementLedgerEntries: List<LedgerEntry>
)

/**
 * Pure Kotlin financial ledger engine enforcing double-entry bookkeeping,
 * balance conservation, overflow prevention, and ownership isolation.
 */
object LedgerEngine {

    /**
     * Validates a transaction against business and accounting rules.
     * Throws [IllegalArgumentException] or [IllegalStateException] if validation fails.
     */
    fun validateTransaction(
        transaction: Transaction,
        accounts: Map<String, Account>,
        householdMembers: List<HouseholdMember>? = null
    ) {
        // 1. Amount validity
        require(transaction.amount.isPositive()) {
            "Transaction amount must be strictly positive (> 0). Found: ${transaction.amount.minorUnits}"
        }

        // 2. Source Account existence & currency alignment
        val sourceAccount = accounts[transaction.accountId]
            ?: throw IllegalArgumentException("Source account '${transaction.accountId}' not found in registry.")

        require(transaction.amount.currency.code == sourceAccount.currency.code) {
            "Transaction currency '${transaction.amount.currency.code}' does not match source account currency '${sourceAccount.currency.code}'."
        }

        // 3. Ownership Isolation
        if (sourceAccount.ownershipType == OwnershipType.PERSONAL) {
            require(transaction.ownershipType == OwnershipType.PERSONAL) {
                "Cannot record a household transaction on a personal account."
            }
            require(transaction.ownerProfileId == sourceAccount.profileId) {
                "Transaction owner profile '${transaction.ownerProfileId}' does not match account profile '${sourceAccount.profileId}'."
            }
            require(transaction.householdId == null) {
                "Personal transaction must not specify a householdId."
            }
        } else {
            require(transaction.ownershipType == OwnershipType.HOUSEHOLD) {
                "Cannot record a personal transaction on a household account."
            }
            require(transaction.householdId == sourceAccount.householdId) {
                "Transaction householdId '${transaction.householdId}' does not match account householdId '${sourceAccount.householdId}'."
            }
        }

        // 4. Type-specific rules
        when (transaction.type) {
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                require(transaction.transferDetails == null) {
                    "Non-transfer transactions (${transaction.type}) must not contain transferDetails."
                }
            }

            TransactionType.TRANSFER -> {
                val details = transaction.transferDetails
                    ?: throw IllegalArgumentException("Transfer transaction must contain non-null transferDetails.")

                require(details.destinationAccountId != transaction.accountId) {
                    "Transfer source and destination accounts must be distinct. Given: '${transaction.accountId}'."
                }

                val destAccount = accounts[details.destinationAccountId]
                    ?: throw IllegalArgumentException("Destination account '${details.destinationAccountId}' not found in registry.")

                require(details.destinationAmount.isPositive()) {
                    "Transfer destination amount must be strictly positive (> 0)."
                }

                require(details.destinationAmount.currency.code == destAccount.currency.code) {
                    "Transfer destination amount currency '${details.destinationAmount.currency.code}' does not match destination account currency '${destAccount.currency.code}'."
                }

                val isSameCurrency = sourceAccount.currency.code == destAccount.currency.code

                if (isSameCurrency) {
                    require(transaction.amount.minorUnits == details.destinationAmount.minorUnits) {
                        "Same-currency transfer must preserve value. Source minor units: ${transaction.amount.minorUnits}, Dest: ${details.destinationAmount.minorUnits}."
                    }
                } else {
                    val rate = details.appliedExchangeRate
                        ?: throw IllegalArgumentException("Cross-currency transfer from '${sourceAccount.currency.code}' to '${destAccount.currency.code}' requires an appliedExchangeRate.")

                    require(rate > BigDecimal.ZERO) {
                        "Applied exchange rate must be strictly positive (> 0)."
                    }

                    // Verify destination amount matches calculated converted amount
                    val expectedDestAmount = transaction.amount.convertTo(destAccount.currency, rate)
                    require(expectedDestAmount.minorUnits == details.destinationAmount.minorUnits) {
                        "Transfer destination amount (${details.destinationAmount.minorUnits}) does not match conversion result (${expectedDestAmount.minorUnits}) using rate $rate."
                    }
                }

                // Transfer Ownership Rules
                validateTransferOwnership(sourceAccount, destAccount, householdMembers)
            }
        }
    }

    private fun validateTransferOwnership(
        source: Account,
        dest: Account,
        householdMembers: List<HouseholdMember>?
    ) {
        if (source.ownershipType == OwnershipType.PERSONAL && dest.ownershipType == OwnershipType.PERSONAL) {
            require(source.profileId == dest.profileId) {
                "Personal transfer between different individuals ('${source.profileId}' and '${dest.profileId}') is not permitted."
            }
        } else if (source.ownershipType == OwnershipType.HOUSEHOLD && dest.ownershipType == OwnershipType.HOUSEHOLD) {
            require(source.householdId == dest.householdId) {
                "Direct transfer between different households ('${source.householdId}' and '${dest.householdId}') is not permitted."
            }
        } else if (householdMembers != null) {
            // Mixed transfer (Personal <-> Household): verify profile is member of household
            val personalProfileId = if (source.ownershipType == OwnershipType.PERSONAL) source.profileId else dest.profileId
            val householdId = if (source.ownershipType == OwnershipType.HOUSEHOLD) source.householdId else dest.householdId

            if (householdId != null) {
                val isMember = householdMembers.any { it.householdId == householdId && it.profileId == personalProfileId }
                require(isMember) {
                    "Profile '$personalProfileId' is not an authorized member of household '$householdId'."
                }
            }
        }
    }

    /**
     * Generates immutable ledger entries for a transaction.
     */
    fun generateLedgerEntries(
        transaction: Transaction,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): List<LedgerEntry> {
        return when (transaction.type) {
            TransactionType.INCOME -> {
                listOf(
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = transaction.id,
                        accountId = transaction.accountId,
                        signedMinorUnits = transaction.amount.minorUnits, // CREDIT: positive
                        currency = transaction.amount.currency,
                        entryType = LedgerEntryType.CREDIT,
                        timestampEpochMs = transaction.transactionDateEpochMs
                    )
                )
            }

            TransactionType.EXPENSE -> {
                listOf(
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = transaction.id,
                        accountId = transaction.accountId,
                        signedMinorUnits = -transaction.amount.minorUnits, // DEBIT: negative
                        currency = transaction.amount.currency,
                        entryType = LedgerEntryType.DEBIT,
                        timestampEpochMs = transaction.transactionDateEpochMs
                    )
                )
            }

            TransactionType.TRANSFER -> {
                val details = requireNotNull(transaction.transferDetails) {
                    "Transfer details must not be null for TRANSFER type."
                }
                listOf(
                    // Source Account: DEBIT (outflow)
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = transaction.id,
                        accountId = transaction.accountId,
                        signedMinorUnits = -transaction.amount.minorUnits,
                        currency = transaction.amount.currency,
                        entryType = LedgerEntryType.DEBIT,
                        timestampEpochMs = transaction.transactionDateEpochMs
                    ),
                    // Destination Account: CREDIT (inflow)
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = transaction.id,
                        accountId = details.destinationAccountId,
                        signedMinorUnits = details.destinationAmount.minorUnits,
                        currency = details.destinationAmount.currency,
                        entryType = LedgerEntryType.CREDIT,
                        timestampEpochMs = transaction.transactionDateEpochMs
                    )
                )
            }
        }
    }

    /**
     * Calculates the balance of an account from its opening balance and ledger entries.
     * Uses strict [Math.addExact] to guarantee that arithmetic overflow is never silently discarded.
     */
    fun calculateAccountBalance(account: Account, ledgerEntries: List<LedgerEntry>): Money {
        var currentMinorUnits = account.initialBalanceMinorUnits
        for (entry in ledgerEntries) {
            if (entry.accountId == account.id) {
                require(entry.currency.code == account.currency.code) {
                    "Ledger entry currency '${entry.currency.code}' does not match account currency '${account.currency.code}'."
                }
                currentMinorUnits = Math.addExact(currentMinorUnits, entry.signedMinorUnits)
            }
        }
        return Money(currentMinorUnits, account.currency)
    }

    /**
     * Calculates cumulative running balances after each ledger entry sorted chronologically.
     */
    fun calculateRunningBalances(
        account: Account,
        ledgerEntries: List<LedgerEntry>
    ): List<Pair<LedgerEntry, Money>> {
        val accountEntries = ledgerEntries
            .filter { it.accountId == account.id }
            .sortedBy { it.timestampEpochMs }

        var runningMinorUnits = account.initialBalanceMinorUnits
        val result = ArrayList<Pair<LedgerEntry, Money>>(accountEntries.size)

        for (entry in accountEntries) {
            require(entry.currency.code == account.currency.code) {
                "Ledger entry currency '${entry.currency.code}' does not match account currency '${account.currency.code}'."
            }
            runningMinorUnits = Math.addExact(runningMinorUnits, entry.signedMinorUnits)
            result.add(Pair(entry, Money(runningMinorUnits, account.currency)))
        }

        return result
    }

    /**
     * Creates a transaction reversal, producing compensating ledger entries that restore previous balances.
     */
    fun createReversal(
        original: Transaction,
        reversalTransactionId: String,
        timestampEpochMs: Long,
        reason: String,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): ReversalResult {
        require(original.status == TransactionStatus.POSTED) {
            "Cannot reverse transaction '${original.id}' with status '${original.status}'. Only POSTED transactions can be reversed."
        }

        val updatedOriginal = original.copy(
            status = TransactionStatus.REVERSED,
            reversalTransactionId = reversalTransactionId,
            updatedAtEpochMs = timestampEpochMs
        )

        val reversalTransaction = original.copy(
            id = reversalTransactionId,
            transactionDateEpochMs = timestampEpochMs,
            description = "Reversal: ${original.description} ($reason)",
            originalTransactionId = original.id,
            reversalTransactionId = null,
            status = TransactionStatus.POSTED,
            createdAtEpochMs = timestampEpochMs,
            updatedAtEpochMs = timestampEpochMs
        )

        val compensatingEntries = when (original.type) {
            TransactionType.INCOME -> {
                // Compensate Income with a DEBIT of the same magnitude
                listOf(
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = reversalTransactionId,
                        accountId = original.accountId,
                        signedMinorUnits = -original.amount.minorUnits,
                        currency = original.amount.currency,
                        entryType = LedgerEntryType.DEBIT,
                        timestampEpochMs = timestampEpochMs
                    )
                )
            }

            TransactionType.EXPENSE -> {
                // Compensate Expense with a CREDIT of the same magnitude
                listOf(
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = reversalTransactionId,
                        accountId = original.accountId,
                        signedMinorUnits = original.amount.minorUnits,
                        currency = original.amount.currency,
                        entryType = LedgerEntryType.CREDIT,
                        timestampEpochMs = timestampEpochMs
                    )
                )
            }

            TransactionType.TRANSFER -> {
                val details = requireNotNull(original.transferDetails)
                listOf(
                    // Source gets credited back (+amount)
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = reversalTransactionId,
                        accountId = original.accountId,
                        signedMinorUnits = original.amount.minorUnits,
                        currency = original.amount.currency,
                        entryType = LedgerEntryType.CREDIT,
                        timestampEpochMs = timestampEpochMs
                    ),
                    // Destination gets debited back (-destAmount)
                    LedgerEntry(
                        id = idGenerator(),
                        transactionId = reversalTransactionId,
                        accountId = details.destinationAccountId,
                        signedMinorUnits = -details.destinationAmount.minorUnits,
                        currency = details.destinationAmount.currency,
                        entryType = LedgerEntryType.DEBIT,
                        timestampEpochMs = timestampEpochMs
                    )
                )
            }
        }

        return ReversalResult(updatedOriginal, reversalTransaction, compensatingEntries)
    }

    /**
     * Corrects a posted transaction by reversing the original and posting a replacement.
     */
    fun createReplacement(
        original: Transaction,
        replacementTransaction: Transaction,
        reversalTransactionId: String,
        timestampEpochMs: Long,
        reason: String,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): ReplacementResult {
        val reversalResult = createReversal(
            original = original,
            reversalTransactionId = reversalTransactionId,
            timestampEpochMs = timestampEpochMs,
            reason = reason,
            idGenerator = idGenerator
        )

        val finalizedReplacement = replacementTransaction.copy(
            originalTransactionId = original.id,
            status = TransactionStatus.POSTED,
            updatedAtEpochMs = timestampEpochMs
        )

        val replacementEntries = generateLedgerEntries(finalizedReplacement, idGenerator)

        return ReplacementResult(
            reversedOriginalTransaction = reversalResult.updatedOriginalTransaction,
            reversalTransaction = reversalResult.reversalTransaction,
            reversalLedgerEntries = reversalResult.compensatingLedgerEntries,
            replacementTransaction = finalizedReplacement,
            replacementLedgerEntries = replacementEntries
        )
    }

    /**
     * Checks if a pair of transfer entries conserves total value in same-currency transfers.
     */
    fun verifySameCurrencyTransferConservation(sourceEntry: LedgerEntry, destEntry: LedgerEntry): Boolean {
        if (sourceEntry.currency.code != destEntry.currency.code) return false
        return Math.addExact(sourceEntry.signedMinorUnits, destEntry.signedMinorUnits) == 0L
    }
}
