package com.walletq.feature.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.model.OwnershipType
import com.walletq.core.model.TransactionType
import com.walletq.feature.activity.components.AddTransactionSheet
import com.walletq.feature.activity.components.TransactionDetailsSheet
import com.walletq.feature.activity.components.TransactionFilterBar
import com.walletq.feature.activity.components.TransactionListItem
import com.walletq.feature.activity.components.TransferSheet

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactionFormState by viewModel.transactionFormState.collectAsStateWithLifecycle()
    val transferFormState by viewModel.transferFormState.collectAsStateWithLifecycle()

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
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onSnackbarShown()
        }
    }

    val hasActiveFilters = uiState.filterState.searchQuery.isNotBlank() ||
        uiState.filterState.ownershipFilter != OwnershipFilter.ALL ||
        uiState.filterState.selectedType != null ||
        uiState.filterState.selectedAccountId != null ||
        uiState.filterState.dateRangeFilter != DateRangeFilter.ALL

    Scaffold(
        topBar = {
            WalletQTopBar(
                title = "Activity",
                actions = {
                    IconButton(onClick = onOpenTransfer) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Transfer funds"
                        )
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Toggle filters"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onOpenTransfer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "Transfer"
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = onOpenAddTransaction,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = showFilters) {
                TransactionFilterBar(
                    filterState = uiState.filterState,
                    accounts = uiState.accounts,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onOwnershipFilterChanged = onOwnershipFilterChanged,
                    onTypeFilterChanged = onTypeFilterChanged,
                    onAccountFilterChanged = onAccountFilterChanged,
                    onDateRangeFilterChanged = onDateRangeFilterChanged,
                    modifier = Modifier.padding(horizontal = Dimensions.spaceMedium, vertical = Dimensions.spaceSmall)
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
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
                                Button(onClick = onOpenAddTransaction) {
                                    Text("Record Transaction")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimensions.spaceMedium,
                            end = Dimensions.spaceMedium,
                            top = Dimensions.spaceSmall,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                    ) {
                        uiState.groupedTransactions.forEach { group ->
                            item(key = "header_${group.epochDay}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Dimensions.spaceSmall, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.dateLabel,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${group.transactions.size} item(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                    onConfirmReversal = onConfirmReversal
                )
            }
        }

        ActiveActivitySheet.NONE -> Unit
    }
}
