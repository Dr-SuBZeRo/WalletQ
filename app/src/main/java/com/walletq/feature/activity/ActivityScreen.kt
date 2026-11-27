package com.walletq.feature.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQMonetaryTypography
import com.walletq.core.designsystem.WalletQMotion
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQFilterChip
import com.walletq.core.designsystem.component.WalletQLoadingPlaceholder
import com.walletq.core.designsystem.component.WalletQPrimaryButton
import com.walletq.core.designsystem.component.WalletQSectionHeader
import com.walletq.core.designsystem.component.WalletQTextField
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.designsystem.rememberReducedMotion
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionType
import com.walletq.feature.activity.components.ActivityFilterSheet
import com.walletq.feature.activity.components.AddTransactionSheet
import com.walletq.feature.activity.components.TransactionDetailsSheet
import com.walletq.feature.activity.components.TransactionListItem
import com.walletq.feature.activity.components.TransferSheet

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    initialAction: String? = null,
    onInitialActionConsumed: () -> Unit = {},
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactionFormState by viewModel.transactionFormState.collectAsStateWithLifecycle()
    val transferFormState by viewModel.transferFormState.collectAsStateWithLifecycle()

    LaunchedEffect(initialAction) {
        when (initialAction) {
            "EXPENSE" -> {
                viewModel.openAddTransactionSheet(TransactionType.EXPENSE)
                onInitialActionConsumed()
            }
            "INCOME" -> {
                viewModel.openAddTransactionSheet(TransactionType.INCOME)
                onInitialActionConsumed()
            }
            "TRANSFER" -> {
                viewModel.openTransferSheet()
                onInitialActionConsumed()
            }
        }
    }

    val selectedAccount = transactionFormState.selectedAccountId?.let { uiState.accountsById[it] }
    val filteredCategories = remember(
        uiState.categories,
        transactionFormState.transactionType,
        selectedAccount
    ) {
        uiState.categories.filter { category ->
            category.transactionType == transactionFormState.transactionType &&
                viewModel.isCategoryValidForAccount(category, selectedAccount)
        }
    }

    ActivityScreenContent(
        uiState = uiState,
        transactionFormState = transactionFormState,
        transferFormState = transferFormState,
        filteredCategories = filteredCategories,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onOwnershipFilterChanged = viewModel::onOwnershipFilterChanged,
        onTypeFilterChanged = viewModel::onTypeFilterChanged,
        onAccountFilterChanged = viewModel::onAccountFilterChanged,
        onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
        onDateRangeFilterChanged = viewModel::onDateRangeFilterChanged,
        onClearFilters = viewModel::clearFilters,
        onOpenAddTransaction = { viewModel.openAddTransactionSheet(TransactionType.EXPENSE) },
        onOpenTransfer = viewModel::openTransferSheet,
        onOpenTransactionDetails = viewModel::openTransactionDetails,
        onDismissSheet = viewModel::dismissSheet,
        onSnackbarShown = viewModel::clearSnackbarMessage,
        onFormTypeChanged = viewModel::onFormTypeChanged,
        onFormOwnershipContextChanged = viewModel::onFormOwnershipContextChanged,
        onFormAccountSelected = viewModel::onFormAccountSelected,
        onFormAmountChanged = viewModel::onFormAmountChanged,
        onFormCategorySelected = viewModel::onFormCategorySelected,
        onFormDateSelected = viewModel::onFormDateSelected,
        onFormDescriptionChanged = viewModel::onFormDescriptionChanged,
        onFormNotesChanged = viewModel::onFormNotesChanged,
        onSubmitTransaction = viewModel::submitIncomeOrExpense,
        onTransferSourceSelected = viewModel::onTransferSourceAccountSelected,
        onTransferDestinationSelected = viewModel::onTransferDestinationAccountSelected,
        onTransferAmountChanged = viewModel::onTransferAmountChanged,
        onTransferExchangeRateChanged = viewModel::onTransferExchangeRateChanged,
        onTransferDateSelected = viewModel::onTransferDateSelected,
        onTransferNotesChanged = viewModel::onTransferNotesChanged,
        onSubmitTransfer = viewModel::submitTransfer,
        onConfirmReversal = viewModel::reverseSelectedTransaction,
        modifier = modifier
    )
}

@Composable
fun ActivityScreenContent(
    uiState: ActivityUiState,
    transactionFormState: TransactionFormState,
    transferFormState: TransferFormState,
    filteredCategories: List<com.walletq.core.model.Category>,
    onSearchQueryChanged: (String) -> Unit,
    onOwnershipFilterChanged: (OwnershipFilter) -> Unit,
    onTypeFilterChanged: (TransactionType?) -> Unit,
    onAccountFilterChanged: (String?) -> Unit,
    onDateRangeFilterChanged: (DateRangeFilter) -> Unit,
    onClearFilters: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenTransactionDetails: (String) -> Unit,
    onDismissSheet: () -> Unit,
    onSnackbarShown: () -> Unit,
    onFormTypeChanged: (TransactionType) -> Unit,
    onFormOwnershipContextChanged: (OwnershipType) -> Unit,
    onFormAccountSelected: (String) -> Unit,
    onFormAmountChanged: (String) -> Unit,
    onFormCategorySelected: (String) -> Unit,
    onFormDateSelected: (Long) -> Unit,
    onFormDescriptionChanged: (String) -> Unit,
    onFormNotesChanged: (String) -> Unit,
    onSubmitTransaction: () -> Unit,
    onTransferSourceSelected: (String) -> Unit,
    onTransferDestinationSelected: (String) -> Unit,
    onTransferAmountChanged: (String) -> Unit,
    onTransferExchangeRateChanged: (String) -> Unit,
    onTransferDateSelected: (Long) -> Unit,
    onTransferNotesChanged: (String) -> Unit,
    onSubmitTransfer: () -> Unit,
    onConfirmReversal: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCategoryFilterChanged: (String?) -> Unit = {}
) {
    val tokens = WalletQThemeTokens.colors
    val reducedMotion = rememberReducedMotion()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onSnackbarShown()
        }
    }

    val activeFilterCount = uiState.filterState.activeFilterCount
    val hasActiveFilters = uiState.filterState.searchQuery.isNotBlank() || activeFilterCount > 0

    Scaffold(
        containerColor = tokens.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WalletQTopBar(
                title = "Activity",
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search transactions",
                            tint = if (showSearch || uiState.filterState.searchQuery.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                tokens.secondaryText
                            }
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge {
                                        Text(activeFilterCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = "Open activity filters",
                                tint = if (activeFilterCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    tokens.secondaryText
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expandable Search Bar
            AnimatedVisibility(
                visible = showSearch,
                enter = WalletQMotion.contentEnter(reducedMotion),
                exit = WalletQMotion.contentExit(reducedMotion)
            ) {
                WalletQTextField(
                    value = uiState.filterState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = "Search",
                    placeholder = "Description, notes, account, or category...",
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.filterState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.padding(
                        horizontal = Dimensions.screenHorizontalPadding,
                        vertical = Dimensions.spaceXSmall
                    )
                )
            }

            // Compact Monthly Summary Card
            uiState.monthlySummary?.let { summary ->
                ActivityMonthlySummaryCard(
                    summary = summary,
                    transactionCount = uiState.transactions.size,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.padding(
                        horizontal = Dimensions.screenHorizontalPadding,
                        vertical = Dimensions.spaceXSmall
                    )
                )
            }

            // Horizontally Scrollable Quick Filter Chips
            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = Dimensions.screenHorizontalPadding,
                    vertical = Dimensions.spaceXSmall
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
            ) {
                items(OwnershipFilter.entries) { ownership ->
                    WalletQFilterChip(
                        selected = uiState.filterState.ownershipFilter == ownership,
                        label = ownership.label,
                        onClick = { onOwnershipFilterChanged(ownership) }
                    )
                }
                items(TransactionType.entries) { type ->
                    val label = when (type) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.TRANSFER -> "Transfer"
                    }
                    WalletQFilterChip(
                        selected = uiState.filterState.selectedType == type,
                        label = label,
                        onClick = {
                            onTypeFilterChanged(if (uiState.filterState.selectedType == type) null else type)
                        }
                    )
                }
                item {
                    WalletQFilterChip(
                        selected = activeFilterCount > 0,
                        label = if (activeFilterCount > 0) "More ($activeFilterCount)" else "More Filters",
                        onClick = { showFilterSheet = true }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    WalletQLoadingPlaceholder(count = 4)
                }

                uiState.errorMessage != null -> {
                    WalletQEmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Unable to Load Activity",
                        message = uiState.errorMessage,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.groupedTransactions.isEmpty() -> {
                    WalletQEmptyState(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = if (hasActiveFilters) "No Matching Transactions" else "No Transactions Recorded",
                        message = if (hasActiveFilters) {
                            "Try adjusting your search query or filters."
                        } else {
                            "Track income, expenses, and transfers with double-entry accuracy."
                        },
                        actionButton = {
                            if (hasActiveFilters) {
                                TextButton(onClick = onClearFilters) {
                                    Text("Clear Filters")
                                }
                            } else {
                                WalletQPrimaryButton(
                                    text = "Record Transaction",
                                    onClick = onOpenAddTransaction
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimensions.screenHorizontalPadding,
                            end = Dimensions.screenHorizontalPadding,
                            top = Dimensions.spaceSmall,
                            bottom = Dimensions.spaceXXLarge
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                    ) {
                        uiState.groupedTransactions.forEach { group ->
                            item(key = "header_${group.epochDay}") {
                                WalletQSectionHeader(
                                    title = group.dateLabel,
                                    trailingText = "${group.transactions.size} item(s)",
                                    modifier = Modifier.padding(top = Dimensions.spaceSmall)
                                )
                            }

                            items(
                                items = group.transactions,
                                key = { it.id }
                            ) { transaction ->
                                val sourceAccount = uiState.accountsById[transaction.accountId]
                                val destAccount = transaction.transferDetails?.destinationAccountId?.let {
                                    uiState.accountsById[it]
                                }
                                val category = transaction.categoryId?.let { uiState.categoriesById[it] }

                                TransactionListItem(
                                    transaction = transaction,
                                    sourceAccount = sourceAccount,
                                    destinationAccount = destAccount,
                                    category = category,
                                    onClick = { onOpenTransactionDetails(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ActivityFilterSheet(
            filterState = uiState.filterState,
            accounts = uiState.accountsById.values.toList(),
            categories = uiState.categories,
            onOwnershipFilterChanged = onOwnershipFilterChanged,
            onTypeFilterChanged = onTypeFilterChanged,
            onDateRangeFilterChanged = onDateRangeFilterChanged,
            onAccountFilterChanged = onAccountFilterChanged,
            onCategoryFilterChanged = onCategoryFilterChanged,
            onClearAll = onClearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    when (uiState.activeSheet) {
        ActiveActivitySheet.ADD_TRANSACTION -> {
            AddTransactionSheet(
                formState = transactionFormState,
                accounts = uiState.accounts,
                filteredCategories = filteredCategories,
                onDismiss = onDismissSheet,
                onTypeChanged = onFormTypeChanged,
                onOwnershipContextChanged = onFormOwnershipContextChanged,
                onAccountSelected = onFormAccountSelected,
                onAmountChanged = onFormAmountChanged,
                onCategorySelected = onFormCategorySelected,
                onDateSelected = onFormDateSelected,
                onDescriptionChanged = onFormDescriptionChanged,
                onNotesChanged = onFormNotesChanged,
                onSubmit = onSubmitTransaction
            )
        }

        ActiveActivitySheet.TRANSFER -> {
            TransferSheet(
                formState = transferFormState,
                accounts = uiState.accounts,
                onDismiss = onDismissSheet,
                onSourceAccountSelected = onTransferSourceSelected,
                onDestinationAccountSelected = onTransferDestinationSelected,
                onAmountChanged = onTransferAmountChanged,
                onExchangeRateChanged = onTransferExchangeRateChanged,
                onDateSelected = onTransferDateSelected,
                onNotesChanged = onTransferNotesChanged,
                onSubmit = onSubmitTransfer
            )
        }

        ActiveActivitySheet.DETAILS -> {
            val selectedTx = uiState.selectedTransaction
            if (selectedTx != null) {
                TransactionDetailsSheet(
                    transaction = selectedTx,
                    accountsById = uiState.accountsById,
                    categoriesById = uiState.categoriesById,
                    householdsById = uiState.householdsById,
                    ledgerEntries = uiState.selectedTransactionLedgerEntries,
                    isReversing = uiState.isReversing,
                    onDismiss = onDismissSheet,
                    onConfirmReversal = { reason -> onConfirmReversal(reason) }
                )
            }
        }

        ActiveActivitySheet.NONE -> Unit
    }
}

@Composable
private fun ActivityMonthlySummaryCard(
    summary: ActivityMonthlySummary,
    transactionCount: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = WalletQThemeTokens.colors
    val incomeText = Money(summary.incomeMinorUnits, summary.currency).format()
    val expenseText = Money(summary.expenseMinorUnits, summary.currency).format()
    val netMoney = Money(summary.netCashFlowMinorUnits, summary.currency)
    val netText = (if (summary.netCashFlowMinorUnits > 0L) "+" else "") + netMoney.format()
    val netColor = if (summary.netCashFlowMinorUnits >= 0L) tokens.success else tokens.error

    WalletQCard(
        elevated = true,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This Month (${summary.currency.code})",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.secondaryText
            )
            Text(
                text = "$transactionCount shown",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.secondaryText
            )
        }
        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryMetricColumn(
                label = "Income",
                value = incomeText,
                valueColor = tokens.success,
                reducedMotion = reducedMotion,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricColumn(
                label = "Expenses",
                value = expenseText,
                valueColor = tokens.error,
                reducedMotion = reducedMotion,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricColumn(
                label = "Net",
                value = netText,
                valueColor = netColor,
                reducedMotion = reducedMotion,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val tokens = WalletQThemeTokens.colors
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.secondaryText
        )
        AnimatedContent(
            targetState = value,
            transitionSpec = WalletQMotion.discreteValueTransition(reducedMotion),
            label = "activity_summary_$label"
        ) { text ->
            Text(
                text = text,
                style = WalletQMonetaryTypography.small,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
