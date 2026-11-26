package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneOffset

class BudgetCalculationEngineTest {

    private val feb2024Boundaries = BudgetCalculationEngine.monthBoundaries(
        yearMonth = YearMonth.of(2024, 2), // Leap year
        zoneId = ZoneOffset.UTC
    )

    private fun baseBudget(
        allocations: List<BudgetCategoryAllocation> = emptyList(),
        allocatedMinor: Long = 100000L,
        currency: Currency = Currency.INR,
        ownershipType: OwnershipType = OwnershipType.PERSONAL,
        profileId: String? = "profile-1",
        householdId: String? = null
    ) = Budget(
        id = "budget-1",
        name = "Feb Budget",
        period = BudgetPeriod.MONTHLY,
        startDateEpochMs = feb2024Boundaries.first,
        endDateEpochMs = feb2024Boundaries.second,
        currency = currency,
        allocatedAmountMinorUnits = allocatedMinor,
        ownershipType = ownershipType,
        profileId = profileId,
        householdId = householdId,
        createdAtEpochMs = feb2024Boundaries.first,
        updatedAtEpochMs = feb2024Boundaries.first,
        categoryAllocations = allocations
    )

    private fun expenseTx(
        id: String,
        amountMinor: Long,
        dateMs: Long = feb2024Boundaries.first + 1000L,
        categoryId: String? = "cat-food",
        currency: Currency = Currency.INR,
        status: TransactionStatus = TransactionStatus.POSTED,
        ownershipType: OwnershipType = OwnershipType.PERSONAL,
        profileId: String = "profile-1",
        householdId: String? = null,
        originalTransactionId: String? = null,
        reversalTransactionId: String? = null
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(amountMinor, currency),
        accountId = "acc-1",
        categoryId = categoryId,
        transactionDateEpochMs = dateMs,
        description = "Tx $id",
        ownershipType = ownershipType,
        ownerProfileId = profileId,
        householdId = householdId,
        status = status,
        originalTransactionId = originalTransactionId,
        reversalTransactionId = reversalTransactionId,
        createdAtEpochMs = dateMs,
        updatedAtEpochMs = dateMs
    )

    @Test
    fun calculateBudgetSpending_excludesIncomeTransfersReversedAndReversals() {
        val budget = baseBudget(allocatedMinor = 50000L) // ₹ 500.00
        val midMonth = feb2024Boundaries.first + 86_400_000L

        val validExpense = expenseTx("tx-exp", 20000L, midMonth)
        val income = validExpense.copy(id = "tx-inc", type = TransactionType.INCOME, amount = Money(99000L, Currency.INR))
        val transfer = validExpense.copy(
            id = "tx-trf",
            type = TransactionType.TRANSFER,
            amount = Money(15000L, Currency.INR),
            transferDetails = TransferDetails("acc-2", Money(15000L, Currency.INR))
        )
        val reversedOriginal = expenseTx(
            id = "tx-rev-orig",
            amountMinor = 12000L,
            dateMs = midMonth,
            status = TransactionStatus.REVERSED,
            reversalTransactionId = "tx-rev-comp"
        )
        val compensatingReversal = expenseTx(
            id = "tx-rev-comp",
            amountMinor = 12000L,
            dateMs = midMonth + 1000L,
            status = TransactionStatus.POSTED,
            originalTransactionId = "tx-rev-orig"
        )
        val differentCurrency = expenseTx("tx-usd", 10000L, midMonth, currency = Currency.USD)
        val otherProfile = expenseTx("tx-other", 10000L, midMonth, profileId = "profile-2")

        val summary = BudgetCalculationEngine.calculateBudgetSpending(
            budget = budget,
            transactions = listOf(
                validExpense,
                income,
                transfer,
                reversedOriginal,
                compensatingReversal,
                differentCurrency,
                otherProfile
            )
        )

        assertEquals(20000L, summary.actualSpendingMinorUnits)
        assertEquals(30000L, summary.remainingMinorUnits)
        assertEquals(0L, summary.overBudgetMinorUnits)
        assertEquals(40, summary.percentageConsumed)
    }

    @Test
    fun calculateBudgetSpending_respectsExactLeapMonthBoundaries() {
        val (startMs, endMs) = feb2024Boundaries
        val budget = baseBudget(allocatedMinor = 50000L)

        val beforeStart = expenseTx("tx-before", 5000L, startMs - 1L)
        val atStart = expenseTx("tx-start", 10000L, startMs)
        val atEnd = expenseTx("tx-end", 15000L, endMs)
        val afterEnd = expenseTx("tx-after", 5000L, endMs + 1L)

        val summary = BudgetCalculationEngine.calculateBudgetSpending(
            budget = budget,
            transactions = listOf(beforeStart, atStart, atEnd, afterEnd)
        )

        assertEquals(25000L, summary.actualSpendingMinorUnits)
    }

    @Test
    fun calculateBudgetSpending_tracksCategoryAllocationsAndOverBudget() {
        val allocFood = BudgetCategoryAllocation("a1", "budget-1", "cat-food", 20000L)
        val allocTransport = BudgetCategoryAllocation("a2", "budget-1", "cat-transport", 10000L)
        val budget = baseBudget(
            allocatedMinor = 30000L,
            allocations = listOf(allocFood, allocTransport)
        )

        val t1 = expenseTx("t1", 25000L, categoryId = "cat-food")
        val t2 = expenseTx("t2", 8000L, categoryId = "cat-transport")
        val tUnallocated = expenseTx("t3", 5000L, categoryId = "cat-other")

        val summary = BudgetCalculationEngine.calculateBudgetSpending(budget, listOf(t1, t2, tUnallocated))

        assertEquals(33000L, summary.actualSpendingMinorUnits)
        assertEquals(0L, summary.remainingMinorUnits)
        assertEquals(3000L, summary.overBudgetMinorUnits)
        assertEquals(110, summary.percentageConsumed)

        val foodSum = summary.categorySummaries.first { it.categoryId == "cat-food" }
        assertEquals(25000L, foodSum.actualSpendingMinorUnits)
        assertEquals(5000L, foodSum.overBudgetMinorUnits)
        assertEquals(125, foodSum.percentageConsumed)
    }

    @Test(expected = ArithmeticException::class)
    fun calculateBudgetSpending_throwsOnMinorUnitOverflow() {
        val budget = baseBudget(allocatedMinor = Long.MAX_VALUE)
        val t1 = expenseTx("t1", Long.MAX_VALUE - 10L)
        val t2 = expenseTx("t2", 20L)

        BudgetCalculationEngine.calculateBudgetSpending(budget, listOf(t1, t2))
    }
}
