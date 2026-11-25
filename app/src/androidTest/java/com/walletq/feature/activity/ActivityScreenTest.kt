package com.walletq.feature.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.walletq.core.designsystem.WalletQTheme
import com.walletq.core.model.Account
import com.walletq.core.model.AccountType
import com.walletq.core.model.Category
import com.walletq.core.model.Currency
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_displaysTitleAndRecordAction() {
        var addClicked = false
        composeRule.setContent {
            WalletQTheme {
                ActivityScreenContent(
                    uiState = ActivityUiState(isLoading = false),
                    transactionFormState = TransactionFormState(),
                    transferFormState = TransferFormState(),
                    filteredCategories = emptyList(),
                    onSearchQueryChanged = {},
                    onOwnershipFilterChanged = {},
                    onTypeFilterChanged = {},
                    onAccountFilterChanged = {},
                    onDateRangeFilterChanged = {},
                    onClearFilters = {},
                    onOpenAddTransaction = { addClicked = true },
                    onOpenTransfer = {},
                    onOpenTransactionDetails = {},
                    onDismissSheet = {},
                    onSnackbarShown = {},
                    onFormTypeChanged = {},
                    onFormOwnershipContextChanged = {},
                    onFormAccountSelected = {},
                    onFormAmountChanged = {},
                    onFormCategorySelected = {},
                    onFormDateSelected = {},
                    onFormDescriptionChanged = {},
                    onFormNotesChanged = {},
                    onSubmitTransaction = {},
                    onTransferSourceSelected = {},
                    onTransferDestinationSelected = {},
                    onTransferAmountChanged = {},
                    onTransferExchangeRateChanged = {},
                    onTransferDateSelected = {},
                    onTransferNotesChanged = {},
                    onSubmitTransfer = {},
                    onConfirmReversal = {}
                )
            }
        }

        composeRule.onNodeWithText("No Transactions Recorded").assertIsDisplayed()
        composeRule.onNodeWithText("Record Transaction").performClick()
        assertTrue(addClicked)
    }

    @Test
    fun populatedState_displaysGroupedTransactionsAndReversalBadge() {
        val account = Account(
            id = "acc-1",
            profileId = "default-personal",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "Wallet Cash",
            type = AccountType.CASH,
            currency = Currency.INR,
            initialBalanceMinorUnits = 50000L,
            currentBalanceMinorUnits = 45000L,
            isArchived = false,
            createdAtEpochMs = 1000L
        )
        val category = Category(
            id = "cat-food",
            name = "Food & Dining",
            transactionType = TransactionType.EXPENSE,
            iconIdentifier = "restaurant",
            colorIdentifier = "#FF5722",
            isSystemCategory = true
        )
        val tx = Transaction(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amount = Money(5000L, Currency.INR),
            accountId = "acc-1",
            categoryId = "cat-food",
            transactionDateEpochMs = System.currentTimeMillis(),
            description = "Morning Coffee",
            ownershipType = OwnershipType.PERSONAL,
            ownerProfileId = "default-personal",
            status = TransactionStatus.REVERSED,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        val uiState = ActivityUiState(
            isLoading = false,
            transactions = listOf(tx),
            groupedTransactions = listOf(
                TransactionDateGroup(epochDay = 1L, dateLabel = "Today", transactions = listOf(tx))
            ),
            accounts = listOf(account),
            accountsById = mapOf(account.id to account),
            categories = listOf(category),
            categoriesById = mapOf(category.id to category)
        )

        var openedTxId: String? = null
        composeRule.setContent {
            WalletQTheme {
                ActivityScreenContent(
                    uiState = uiState,
                    transactionFormState = TransactionFormState(),
                    transferFormState = TransferFormState(),
                    filteredCategories = listOf(category),
                    onSearchQueryChanged = {},
                    onOwnershipFilterChanged = {},
                    onTypeFilterChanged = {},
                    onAccountFilterChanged = {},
                    onDateRangeFilterChanged = {},
                    onClearFilters = {},
                    onOpenAddTransaction = {},
                    onOpenTransfer = {},
                    onOpenTransactionDetails = { openedTxId = it },
                    onDismissSheet = {},
                    onSnackbarShown = {},
                    onFormTypeChanged = {},
                    onFormOwnershipContextChanged = {},
                    onFormAccountSelected = {},
                    onFormAmountChanged = {},
                    onFormCategorySelected = {},
                    onFormDateSelected = {},
                    onFormDescriptionChanged = {},
                    onFormNotesChanged = {},
                    onSubmitTransaction = {},
                    onTransferSourceSelected = {},
                    onTransferDestinationSelected = {},
                    onTransferAmountChanged = {},
                    onTransferExchangeRateChanged = {},
                    onTransferDateSelected = {},
                    onTransferNotesChanged = {},
                    onSubmitTransfer = {},
                    onConfirmReversal = {}
                )
            }
        }

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Morning Coffee").assertIsDisplayed()
        composeRule.onNodeWithText("REVERSED").assertIsDisplayed()
        composeRule.onNodeWithText("Morning Coffee").performClick()
        assertEquals("tx-1", openedTxId)
    }
}
