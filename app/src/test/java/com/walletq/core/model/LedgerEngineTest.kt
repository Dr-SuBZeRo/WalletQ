package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class LedgerEngineTest {

    private lateinit var personalAccountInr: Account
    private lateinit var personalAccountCashInr: Account
    private lateinit var personalAccountUsd: Account
    private lateinit var personalAccountJpy: Account
    private lateinit var householdAccountInr: Account
    private lateinit var otherPersonAccountInr: Account
    private lateinit var otherHouseholdAccountInr: Account

    private lateinit var accountsMap: Map<String, Account>
    private lateinit var householdMembers: List<HouseholdMember>

    @Before
    fun setUp() {
        personalAccountInr = Account(
            id = "acc-personal-bank-inr",
            profileId = "profile-1",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "Personal Bank INR",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 100000L, // ₹ 1,000.00
            currentBalanceMinorUnits = 100000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        personalAccountCashInr = Account(
            id = "acc-personal-cash-inr",
            profileId = "profile-1",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "Personal Cash INR",
            type = AccountType.CASH,
            currency = Currency.INR,
            initialBalanceMinorUnits = 10000L, // ₹ 100.00
            currentBalanceMinorUnits = 10000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        personalAccountUsd = Account(
            id = "acc-personal-usd",
            profileId = "profile-1",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "Personal USD Forex",
            type = AccountType.BANK,
            currency = Currency.USD,
            initialBalanceMinorUnits = 5000L, // $ 50.00
            currentBalanceMinorUnits = 5000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        personalAccountJpy = Account(
            id = "acc-personal-jpy",
            profileId = "profile-1",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "Personal JPY Cash",
            type = AccountType.CASH,
            currency = Currency.JPY,
            initialBalanceMinorUnits = 0L, // ¥ 0 (0 decimals)
            currentBalanceMinorUnits = 0L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        householdAccountInr = Account(
            id = "acc-household-inr",
            profileId = "profile-1",
            householdId = "hh-1",
            ownershipType = OwnershipType.HOUSEHOLD,
            name = "Joint Household Bank",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 500000L, // ₹ 5,000.00
            currentBalanceMinorUnits = 500000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        otherPersonAccountInr = Account(
            id = "acc-other-person-inr",
            profileId = "profile-2",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "Friend's Personal Bank",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 50000L,
            currentBalanceMinorUnits = 50000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        otherHouseholdAccountInr = Account(
            id = "acc-other-hh-inr",
            profileId = "profile-2",
            householdId = "hh-2",
            ownershipType = OwnershipType.HOUSEHOLD,
            name = "Other Household Bank",
            type = AccountType.BANK,
            currency = Currency.INR,
            initialBalanceMinorUnits = 200000L,
            currentBalanceMinorUnits = 200000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )

        accountsMap = listOf(
            personalAccountInr,
            personalAccountCashInr,
            personalAccountUsd,
            personalAccountJpy,
            householdAccountInr,
            otherPersonAccountInr,
            otherHouseholdAccountInr
        ).associateBy { it.id }

        householdMembers = listOf(
            HouseholdMember("hh-1", "profile-1", HouseholdRole.OWNER, 1700000000000L),
            HouseholdMember("hh-2", "profile-2", HouseholdRole.OWNER, 1700000000000L)
        )
    }

    @Test
    fun income_generatesCreditEntry_andIncreasesBalance() {
        val incomeTx = Transaction(
            id = "tx-inc-1",
            type = TransactionType.INCOME,
            amount = Money(25000L, Currency.INR), // ₹ 250.00
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000010000L,
            description = "Monthly Freelance Dividend",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000010000L,
            updatedAtEpochMs = 1700000010000L
        )

        LedgerEngine.validateTransaction(incomeTx, accountsMap)
        val entries = LedgerEngine.generateLedgerEntries(incomeTx)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals(incomeTx.id, entry.transactionId)
        assertEquals(personalAccountInr.id, entry.accountId)
        assertEquals(25000L, entry.signedMinorUnits)
        assertEquals(LedgerEntryType.CREDIT, entry.entryType)
        assertEquals(Currency.INR, entry.currency)

        val newBalance = LedgerEngine.calculateAccountBalance(personalAccountInr, entries)
        // Opening ₹ 1,000.00 + ₹ 250.00 = ₹ 1,250.00 (125,000 minor units)
        assertEquals(125000L, newBalance.minorUnits)
        assertEquals("₹ 1,250.00", newBalance.format())
    }

    @Test
    fun expense_generatesDebitEntry_andDecreasesBalance() {
        val expenseTx = Transaction(
            id = "tx-exp-1",
            type = TransactionType.EXPENSE,
            amount = Money(35000L, Currency.INR), // ₹ 350.00
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000020000L,
            description = "Grocery Store",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000020000L,
            updatedAtEpochMs = 1700000020000L
        )

        LedgerEngine.validateTransaction(expenseTx, accountsMap)
        val entries = LedgerEngine.generateLedgerEntries(expenseTx)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals(expenseTx.id, entry.transactionId)
        assertEquals(personalAccountInr.id, entry.accountId)
        assertEquals(-35000L, entry.signedMinorUnits) // Negative for DEBIT
        assertEquals(LedgerEntryType.DEBIT, entry.entryType)

        val newBalance = LedgerEngine.calculateAccountBalance(personalAccountInr, entries)
        // Opening ₹ 1,000.00 - ₹ 350.00 = ₹ 650.00 (65,000 minor units)
        assertEquals(65000L, newBalance.minorUnits)
        assertEquals("₹ 650.00", newBalance.format())
    }

    @Test
    fun sameCurrencyTransfer_generatesDebitAndCredit_andPreservesConservation() {
        val transferTx = Transaction(
            id = "tx-tr-1",
            type = TransactionType.TRANSFER,
            amount = Money(20000L, Currency.INR), // ₹ 200.00
            accountId = personalAccountInr.id,
            transferDetails = TransferDetails(
                destinationAccountId = personalAccountCashInr.id,
                destinationAmount = Money(20000L, Currency.INR)
            ),
            transactionDateEpochMs = 1700000030000L,
            description = "ATM Cash Withdrawal",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000030000L,
            updatedAtEpochMs = 1700000030000L
        )

        LedgerEngine.validateTransaction(transferTx, accountsMap)
        val entries = LedgerEngine.generateLedgerEntries(transferTx)

        assertEquals(2, entries.size)
        val sourceEntry = entries.first { it.accountId == personalAccountInr.id }
        val destEntry = entries.first { it.accountId == personalAccountCashInr.id }

        // Source must be debited: -20,000
        assertEquals(-20000L, sourceEntry.signedMinorUnits)
        assertEquals(LedgerEntryType.DEBIT, sourceEntry.entryType)

        // Destination must be credited: +20,000
        assertEquals(20000L, destEntry.signedMinorUnits)
        assertEquals(LedgerEntryType.CREDIT, destEntry.entryType)

        // Invariant: sum of entries must be 0 (Total system value conserved)
        assertTrue(LedgerEngine.verifySameCurrencyTransferConservation(sourceEntry, destEntry))
        assertEquals(0L, sourceEntry.signedMinorUnits + destEntry.signedMinorUnits)

        // Source balance: ₹ 1,000 - 200 = ₹ 800 (80,000 minor units)
        val sourceBalance = LedgerEngine.calculateAccountBalance(personalAccountInr, entries)
        assertEquals(80000L, sourceBalance.minorUnits)

        // Destination balance: ₹ 100 + 200 = ₹ 300 (30,000 minor units)
        val destBalance = LedgerEngine.calculateAccountBalance(personalAccountCashInr, entries)
        assertEquals(30000L, destBalance.minorUnits)
    }

    @Test
    fun crossCurrencyTransfer_inrToUsd_appliesExchangeRateWithBankersRounding() {
        // Transfer ₹ 8,425.00 INR to USD at rate 0.0118694 USD per INR
        // 8425.00 * 0.01186943 = 100.0000 -> $ 100.00 (10,000 minor units)
        val rate = BigDecimal("0.011869436")
        val inrAmount = Money(842500L, Currency.INR)
        val usdAmount = inrAmount.convertTo(Currency.USD, rate)
        assertEquals(10000L, usdAmount.minorUnits) // $ 100.00

        val transferTx = Transaction(
            id = "tx-tr-cross-1",
            type = TransactionType.TRANSFER,
            amount = inrAmount,
            accountId = personalAccountInr.id,
            transferDetails = TransferDetails(
                destinationAccountId = personalAccountUsd.id,
                destinationAmount = usdAmount,
                appliedExchangeRate = rate
            ),
            transactionDateEpochMs = 1700000040000L,
            description = "Fund USD Travel Wallet",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000040000L,
            updatedAtEpochMs = 1700000040000L
        )

        LedgerEngine.validateTransaction(transferTx, accountsMap)
        val entries = LedgerEngine.generateLedgerEntries(transferTx)

        val sourceEntry = entries.first { it.accountId == personalAccountInr.id }
        val destEntry = entries.first { it.accountId == personalAccountUsd.id }

        assertEquals(-842500L, sourceEntry.signedMinorUnits)
        assertEquals(Currency.INR, sourceEntry.currency)

        assertEquals(10000L, destEntry.signedMinorUnits)
        assertEquals(Currency.USD, destEntry.currency)

        val usdBalance = LedgerEngine.calculateAccountBalance(personalAccountUsd, entries)
        // Opening $50.00 + $100.00 = $150.00 (15,000 minor units)
        assertEquals(15000L, usdBalance.minorUnits)
        assertEquals("$ 150.00", usdBalance.format())
    }

    @Test
    fun crossCurrencyTransfer_usdToJpy_handlesZeroDecimalPrecision() {
        // Transfer $ 10.00 USD to JPY at rate 155.45 JPY per USD
        // 10.00 * 155.45 = 1554.50 -> Banker's rounding to nearest even: 1554 JPY (0 decimals)
        val rate = BigDecimal("155.45")
        val usdAmount = Money(1000L, Currency.USD)
        val jpyAmount = usdAmount.convertTo(Currency.JPY, rate)
        assertEquals(1554L, jpyAmount.minorUnits)
        assertEquals(0, jpyAmount.currency.decimalPlaces)

        val transferTx = Transaction(
            id = "tx-tr-jpy",
            type = TransactionType.TRANSFER,
            amount = usdAmount,
            accountId = personalAccountUsd.id,
            transferDetails = TransferDetails(
                destinationAccountId = personalAccountJpy.id,
                destinationAmount = jpyAmount,
                appliedExchangeRate = rate
            ),
            transactionDateEpochMs = 1700000050000L,
            description = "Exchange USD for Yen",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000050000L,
            updatedAtEpochMs = 1700000050000L
        )

        LedgerEngine.validateTransaction(transferTx, accountsMap)
        val entries = LedgerEngine.generateLedgerEntries(transferTx)

        val jpyEntry = entries.first { it.accountId == personalAccountJpy.id }
        assertEquals(1554L, jpyEntry.signedMinorUnits)
        assertEquals(Currency.JPY, jpyEntry.currency)

        val jpyBalance = LedgerEngine.calculateAccountBalance(personalAccountJpy, entries)
        assertEquals(1554L, jpyBalance.minorUnits)
        assertEquals("¥ 1,554", jpyBalance.format())
    }

    @Test
    fun historicalExchangeRate_canBeUtilizedForCrossCurrencyTransfer() {
        val historicalRate = ExchangeRate(
            id = "rate-usd-inr-2026-11",
            baseCurrencyCode = "USD",
            targetCurrencyCode = "INR",
            rate = BigDecimal("84.20000000"),
            timestampEpochMs = 1700000000000L
        )

        val usdAmount = Money(10000L, Currency.USD) // $ 100.00
        val inrAmount = usdAmount.convertTo(Currency.INR, historicalRate.rate) // ₹ 8,420.00 (842,000 minor units)

        val transferTx = Transaction(
            id = "tx-tr-historical",
            type = TransactionType.TRANSFER,
            amount = usdAmount,
            accountId = personalAccountUsd.id,
            transferDetails = TransferDetails(
                destinationAccountId = personalAccountInr.id,
                destinationAmount = inrAmount,
                appliedExchangeRate = historicalRate.rate,
                exchangeRateReferenceId = historicalRate.id
            ),
            transactionDateEpochMs = historicalRate.timestampEpochMs,
            description = "Remittance from Forex at benchmark rate",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        LedgerEngine.validateTransaction(transferTx, accountsMap)
        assertEquals("rate-usd-inr-2026-11", transferTx.transferDetails?.exchangeRateReferenceId)
    }

    @Test
    fun validation_failsWhenCurrencyMismatchBetweenTransactionAndAccount() {
        val invalidTx = Transaction(
            id = "tx-err-1",
            type = TransactionType.INCOME,
            amount = Money(1000L, Currency.USD), // USD on INR account
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000000000L,
            description = "Illegal currency deposit",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(invalidTx, accountsMap)
            fail("Expected IllegalArgumentException for currency mismatch")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("does not match source account currency") == true)
        }
    }

    @Test
    fun validation_failsWhenAmountIsZeroOrNegative() {
        val zeroTx = Transaction(
            id = "tx-err-zero",
            type = TransactionType.INCOME,
            amount = Money(0L, Currency.INR),
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000000000L,
            description = "Zero deposit",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(zeroTx, accountsMap)
            fail("Expected IllegalArgumentException for zero amount")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("strictly positive") == true)
        }

        val negTx = zeroTx.copy(amount = Money(-1000L, Currency.INR))
        try {
            LedgerEngine.validateTransaction(negTx, accountsMap)
            fail("Expected IllegalArgumentException for negative amount")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("strictly positive") == true)
        }
    }

    @Test
    fun validation_failsWhenTransferSourceAndDestinationAreSameAccount() {
        val selfTransfer = Transaction(
            id = "tx-err-self",
            type = TransactionType.TRANSFER,
            amount = Money(1000L, Currency.INR),
            accountId = personalAccountInr.id,
            transferDetails = TransferDetails(
                destinationAccountId = personalAccountInr.id, // Same account!
                destinationAmount = Money(1000L, Currency.INR)
            ),
            transactionDateEpochMs = 1700000000000L,
            description = "Self loop",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(selfTransfer, accountsMap)
            fail("Expected IllegalArgumentException for self-transfer")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must be distinct") == true)
        }
    }

    @Test(expected = ArithmeticException::class)
    fun arithmeticOverflow_throwsArithmeticException() {
        val hugeAccount = personalAccountInr.copy(initialBalanceMinorUnits = Long.MAX_VALUE - 50L)
        val overflowEntry = LedgerEntry(
            id = "entry-overflow",
            transactionId = "tx-huge",
            accountId = hugeAccount.id,
            signedMinorUnits = 100L, // Causes Long overflow!
            currency = Currency.INR,
            entryType = LedgerEntryType.CREDIT,
            timestampEpochMs = 1700000000000L
        )

        LedgerEngine.calculateAccountBalance(hugeAccount, listOf(overflowEntry))
    }

    @Test
    fun reversal_ofIncome_restoresAccountBalance() {
        val incomeTx = Transaction(
            id = "tx-inc-orig",
            type = TransactionType.INCOME,
            amount = Money(50000L, Currency.INR), // ₹ 500.00
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000000000L,
            description = "Accidental bonus deposit",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        val origEntries = LedgerEngine.generateLedgerEntries(incomeTx)
        val balanceWithIncome = LedgerEngine.calculateAccountBalance(personalAccountInr, origEntries)
        assertEquals(150000L, balanceWithIncome.minorUnits) // ₹ 1,000 + 500 = ₹ 1,500

        // Execute Reversal
        val reversalResult = LedgerEngine.createReversal(
            original = incomeTx,
            reversalTransactionId = "tx-inc-rev",
            timestampEpochMs = 1700000050000L,
            reason = "Duplicated transaction"
        )

        assertEquals(TransactionStatus.REVERSED, reversalResult.updatedOriginalTransaction.status)
        assertEquals("tx-inc-rev", reversalResult.updatedOriginalTransaction.reversalTransactionId)

        val allEntries = origEntries + reversalResult.compensatingLedgerEntries
        val balanceAfterReversal = LedgerEngine.calculateAccountBalance(personalAccountInr, allEntries)

        // Balance perfectly restored to opening balance: ₹ 1,000.00
        assertEquals(100000L, balanceAfterReversal.minorUnits)
    }

    @Test
    fun reversal_ofExpense_restoresAccountBalance() {
        val expenseTx = Transaction(
            id = "tx-exp-orig",
            type = TransactionType.EXPENSE,
            amount = Money(30000L, Currency.INR), // ₹ 300.00
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000000000L,
            description = "Defective merchandise",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        val origEntries = LedgerEngine.generateLedgerEntries(expenseTx)
        val balanceWithExpense = LedgerEngine.calculateAccountBalance(personalAccountInr, origEntries)
        assertEquals(70000L, balanceWithExpense.minorUnits) // ₹ 1,000 - 300 = ₹ 700

        // Execute Reversal
        val reversalResult = LedgerEngine.createReversal(
            original = expenseTx,
            reversalTransactionId = "tx-exp-rev",
            timestampEpochMs = 1700000060000L,
            reason = "Store return"
        )

        val allEntries = origEntries + reversalResult.compensatingLedgerEntries
        val balanceAfterReversal = LedgerEngine.calculateAccountBalance(personalAccountInr, allEntries)

        // Balance restored to ₹ 1,000.00
        assertEquals(100000L, balanceAfterReversal.minorUnits)
    }

    @Test
    fun reversal_ofTransfer_restoresBothAccounts() {
        val transferTx = Transaction(
            id = "tx-tr-orig",
            type = TransactionType.TRANSFER,
            amount = Money(20000L, Currency.INR), // ₹ 200.00
            accountId = personalAccountInr.id,
            transferDetails = TransferDetails(
                destinationAccountId = personalAccountCashInr.id,
                destinationAmount = Money(20000L, Currency.INR)
            ),
            transactionDateEpochMs = 1700000000000L,
            description = "Incorrect withdrawal",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        val origEntries = LedgerEngine.generateLedgerEntries(transferTx)
        val reversalResult = LedgerEngine.createReversal(
            original = transferTx,
            reversalTransactionId = "tx-tr-rev",
            timestampEpochMs = 1700000070000L,
            reason = "Reversed ATM transaction"
        )

        val allEntries = origEntries + reversalResult.compensatingLedgerEntries

        val restoredSourceBalance = LedgerEngine.calculateAccountBalance(personalAccountInr, allEntries)
        val restoredDestBalance = LedgerEngine.calculateAccountBalance(personalAccountCashInr, allEntries)

        // Both accounts restored to opening amounts
        assertEquals(100000L, restoredSourceBalance.minorUnits) // ₹ 1,000.00
        assertEquals(10000L, restoredDestBalance.minorUnits)   // ₹ 100.00
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversal_cannotReverseAlreadyReversedTransaction() {
        val alreadyReversedTx = Transaction(
            id = "tx-already-rev",
            type = TransactionType.INCOME,
            amount = Money(10000L, Currency.INR),
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000000000L,
            description = "Done",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            status = TransactionStatus.REVERSED,
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        LedgerEngine.createReversal(alreadyReversedTx, "rev-2", 1700000010000L, "test")
    }

    @Test
    fun replacement_generatesReversalAndNewPostedTransaction() {
        val originalExpense = Transaction(
            id = "tx-orig-exp",
            type = TransactionType.EXPENSE,
            amount = Money(5000L, Currency.INR), // entered as ₹ 50.00 by mistake
            accountId = personalAccountInr.id,
            transactionDateEpochMs = 1700000000000L,
            description = "Lunch typo",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        val correctedExpense = originalExpense.copy(
            id = "tx-corrected-exp",
            amount = Money(4000L, Currency.INR), // correct amount ₹ 40.00
            description = "Lunch correct"
        )

        val replacementResult = LedgerEngine.createReplacement(
            original = originalExpense,
            replacementTransaction = correctedExpense,
            reversalTransactionId = "tx-reversal",
            timestampEpochMs = 1700000080000L,
            reason = "Amount correction from 50 to 40"
        )

        // Audit Trail Checks
        assertEquals(TransactionStatus.REVERSED, replacementResult.reversedOriginalTransaction.status)
        assertEquals("tx-reversal", replacementResult.reversedOriginalTransaction.reversalTransactionId)

        assertEquals("tx-orig-exp", replacementResult.reversalTransaction.originalTransactionId)
        assertEquals("tx-orig-exp", replacementResult.replacementTransaction.originalTransactionId)
        assertEquals(TransactionStatus.POSTED, replacementResult.replacementTransaction.status)

        // Ledger entries: original (-50), reversal (+50), replacement (-40) => net effect: -40
        val origEntries = LedgerEngine.generateLedgerEntries(originalExpense)
        val totalHistory = origEntries + replacementResult.reversalLedgerEntries + replacementResult.replacementLedgerEntries

        val finalBalance = LedgerEngine.calculateAccountBalance(personalAccountInr, totalHistory)
        // Opening ₹ 1,000.00 - ₹ 40.00 = ₹ 960.00 (96,000 minor units)
        assertEquals(96000L, finalBalance.minorUnits)
    }

    @Test
    fun ownershipValidation_personalAccountRejectsHouseholdTransaction() {
        val illegalTx = Transaction(
            id = "tx-leak-1",
            type = TransactionType.INCOME,
            amount = Money(1000L, Currency.INR),
            accountId = personalAccountInr.id, // PERSONAL account
            transactionDateEpochMs = 1700000000000L,
            description = "Leak",
            ownershipType = OwnershipType.HOUSEHOLD, // Claimed as HOUSEHOLD!
            ownerProfileId = "profile-1",
            householdId = "hh-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(illegalTx, accountsMap)
            fail("Expected IllegalArgumentException for personal account with household transaction")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("household transaction on a personal account") == true)
        }
    }

    @Test
    fun ownershipValidation_householdAccountRejectsPersonalTransaction() {
        val illegalTx = Transaction(
            id = "tx-leak-2",
            type = TransactionType.INCOME,
            amount = Money(1000L, Currency.INR),
            accountId = householdAccountInr.id, // HOUSEHOLD account
            transactionDateEpochMs = 1700000000000L,
            description = "Leak",
            ownershipType = OwnershipType.PERSONAL, // Claimed as PERSONAL!
            ownerProfileId = "profile-1",
            householdId = null,
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(illegalTx, accountsMap)
            fail("Expected IllegalArgumentException for household account with personal transaction")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("personal transaction on a household account") == true)
        }
    }

    @Test
    fun ownershipValidation_personalTransferRejectsCrossIndividualTransfer() {
        val crossPersonTransfer = Transaction(
            id = "tx-cross-person",
            type = TransactionType.TRANSFER,
            amount = Money(5000L, Currency.INR),
            accountId = personalAccountInr.id, // profile-1
            transferDetails = TransferDetails(
                destinationAccountId = otherPersonAccountInr.id, // profile-2
                destinationAmount = Money(5000L, Currency.INR)
            ),
            transactionDateEpochMs = 1700000000000L,
            description = "Cross individual transfer",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(crossPersonTransfer, accountsMap)
            fail("Expected IllegalArgumentException for cross-person transfer")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between different individuals") == true)
        }
    }

    @Test
    fun ownershipValidation_householdTransferRejectsCrossHouseholdTransfer() {
        val crossHhTransfer = Transaction(
            id = "tx-cross-hh",
            type = TransactionType.TRANSFER,
            amount = Money(5000L, Currency.INR),
            accountId = householdAccountInr.id, // hh-1
            transferDetails = TransferDetails(
                destinationAccountId = otherHouseholdAccountInr.id, // hh-2
                destinationAmount = Money(5000L, Currency.INR)
            ),
            transactionDateEpochMs = 1700000000000L,
            description = "Cross household transfer",
            ownershipType = OwnershipType.HOUSEHOLD,
            ownerProfileId = "profile-1",
            householdId = "hh-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        try {
            LedgerEngine.validateTransaction(crossHhTransfer, accountsMap)
            fail("Expected IllegalArgumentException for cross-household transfer")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between different households") == true)
        }
    }

    @Test
    fun ownershipValidation_mixedTransferPermittedOnlyIfProfileIsMember() {
        // Transfer from profile-1 Personal to hh-1 Household (profile-1 IS member of hh-1)
        val validMixedTransfer = Transaction(
            id = "tx-valid-mixed",
            type = TransactionType.TRANSFER,
            amount = Money(5000L, Currency.INR),
            accountId = personalAccountInr.id, // profile-1
            transferDetails = TransferDetails(
                destinationAccountId = householdAccountInr.id, // hh-1
                destinationAmount = Money(5000L, Currency.INR)
            ),
            transactionDateEpochMs = 1700000000000L,
            description = "Contribution to household pool",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "profile-1",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000000000L
        )

        // Should pass
        LedgerEngine.validateTransaction(validMixedTransfer, accountsMap, householdMembers)

        // Transfer from profile-1 Personal to hh-2 Household (profile-1 is NOT member of hh-2)
        val invalidMixedTransfer = validMixedTransfer.copy(
            id = "tx-invalid-mixed",
            transferDetails = TransferDetails(
                destinationAccountId = otherHouseholdAccountInr.id, // hh-2
                destinationAmount = Money(5000L, Currency.INR)
            )
        )

        try {
            LedgerEngine.validateTransaction(invalidMixedTransfer, accountsMap, householdMembers)
            fail("Expected IllegalArgumentException when non-member contributes to household")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("not an authorized member") == true)
        }
    }

    @Test
    fun runningBalances_computesChronologicalProgression() {
        val entry1 = LedgerEntry("e1", "tx1", personalAccountInr.id, 10000L, Currency.INR, LedgerEntryType.CREDIT, 1000L) // +100
        val entry2 = LedgerEntry("e2", "tx2", personalAccountInr.id, -2000L, Currency.INR, LedgerEntryType.DEBIT, 2000L)  // -20
        val entry3 = LedgerEntry("e3", "tx3", personalAccountInr.id, 5000L, Currency.INR, LedgerEntryType.CREDIT, 3000L)  // +50

        // Initial balance = 100,000 (₹ 1,000)
        val progression = LedgerEngine.calculateRunningBalances(personalAccountInr, listOf(entry2, entry1, entry3)) // unsorted input

        assertEquals(3, progression.size)
        // Step 1: 1,000 + 100 = 1,100 (110,000)
        assertEquals(110000L, progression[0].second.minorUnits)
        // Step 2: 1,100 - 20 = 1,080 (108,000)
        assertEquals(108000L, progression[1].second.minorUnits)
        // Step 3: 1,080 + 50 = 1,130 (113,000)
        assertEquals(113000L, progression[2].second.minorUnits)
    }
}
