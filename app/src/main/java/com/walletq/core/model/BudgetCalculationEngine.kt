package com.walletq.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.ZoneId

/**
 * Pure Kotlin budget spending calculation engine.
 *
 * Financial & Accounting Rules:
 * - Only POSTED EXPENSE transactions count toward spending.
 * - INCOME, TRANSFER, REVERSED transactions, and compensating reversal records are excluded.
 * - Strictly matches budget currency and ownership scope (Personal vs Household).
 * - Uses checked integer minor-unit arithmetic (Math.addExact / Math.subtractExact) and BigDecimal;
 *   never uses Float or Double.
 */
object BudgetCalculationEngine {

    /**
     * Returns inclusive [startDateEpochMs, endDateEpochMs] boundaries for [yearMonth] in [zoneId].
     */
    fun monthBoundaries(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Long, Long> {
        val startMs = yearMonth.atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val endMs = yearMonth.plusMonths(1)
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1L
        return startMs to endMs
    }

    fun calculateBudgetSpending(
        budget: Budget,
        transactions: List<Transaction>
    ): BudgetSpendingSummary {
        // Identify compensating reversal transaction IDs so reversed expenses never double-count
        val reversalTransactionIds = transactions
            .mapNotNull { it.reversalTransactionId }
            .toSet()

        val eligibleExpenses = transactions.filter { tx ->
            isEligibleExpense(tx, budget, reversalTransactionIds)
        }

        val categorySummaries = budget.categoryAllocations.map { allocation ->
            val catSpent = eligibleExpenses
                .asSequence()
                .filter { it.categoryId == allocation.categoryId }
                .fold(0L) { acc, tx -> Math.addExact(acc, tx.amount.minorUnits) }

            val catRemaining = if (allocation.allocatedAmountMinorUnits > catSpent) {
                Math.subtractExact(allocation.allocatedAmountMinorUnits, catSpent)
            } else {
                0L
            }
            val catOver = if (catSpent > allocation.allocatedAmountMinorUnits) {
                Math.subtractExact(catSpent, allocation.allocatedAmountMinorUnits)
            } else {
                0L
            }
            CategorySpendingSummary(
                categoryId = allocation.categoryId,
                allocatedMinorUnits = allocation.allocatedAmountMinorUnits,
                actualSpendingMinorUnits = catSpent,
                remainingMinorUnits = catRemaining,
                percentageConsumed = computePercentage(catSpent, allocation.allocatedAmountMinorUnits),
                overBudgetMinorUnits = catOver
            )
        }

        val totalSpent = if (budget.categoryAllocations.isEmpty()) {
            eligibleExpenses.fold(0L) { acc, tx -> Math.addExact(acc, tx.amount.minorUnits) }
        } else {
            val allocatedCategoryIds = budget.categoryAllocations.map { it.categoryId }.toSet()
            eligibleExpenses
                .asSequence()
                .filter { it.categoryId != null && it.categoryId in allocatedCategoryIds }
                .fold(0L) { acc, tx -> Math.addExact(acc, tx.amount.minorUnits) }
        }

        val remaining = if (budget.allocatedAmountMinorUnits > totalSpent) {
            Math.subtractExact(budget.allocatedAmountMinorUnits, totalSpent)
        } else {
            0L
        }
        val overBudget = if (totalSpent > budget.allocatedAmountMinorUnits) {
            Math.subtractExact(totalSpent, budget.allocatedAmountMinorUnits)
        } else {
            0L
        }

        return BudgetSpendingSummary(
            budget = budget,
            allocatedMinorUnits = budget.allocatedAmountMinorUnits,
            actualSpendingMinorUnits = totalSpent,
            remainingMinorUnits = remaining,
            percentageConsumed = computePercentage(totalSpent, budget.allocatedAmountMinorUnits),
            overBudgetMinorUnits = overBudget,
            categorySummaries = categorySummaries
        )
    }

    private fun isEligibleExpense(
        tx: Transaction,
        budget: Budget,
        reversalTransactionIds: Set<String>
    ): Boolean {
        if (tx.status != TransactionStatus.POSTED) return false
        if (tx.type != TransactionType.EXPENSE) return false
        if (tx.id in reversalTransactionIds) return false
        if (tx.amount.currency.code != budget.currency.code) return false
        if (tx.transactionDateEpochMs < budget.startDateEpochMs ||
            tx.transactionDateEpochMs > budget.endDateEpochMs
        ) {
            return false
        }

        return when (budget.ownershipType) {
            OwnershipType.PERSONAL ->
                tx.ownershipType == OwnershipType.PERSONAL &&
                    tx.ownerProfileId == budget.profileId &&
                    tx.householdId == null

            OwnershipType.HOUSEHOLD ->
                tx.ownershipType == OwnershipType.HOUSEHOLD &&
                    tx.householdId == budget.householdId
        }
    }

    private fun computePercentage(spentMinorUnits: Long, allocatedMinorUnits: Long): Int {
        if (allocatedMinorUnits <= 0L) return if (spentMinorUnits > 0L) 100 else 0
        return BigDecimal.valueOf(spentMinorUnits)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(allocatedMinorUnits), 0, RoundingMode.HALF_EVEN)
            .toInt()
    }
}
