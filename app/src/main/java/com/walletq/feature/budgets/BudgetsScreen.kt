package com.walletq.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.ExpenseRed
import com.walletq.core.designsystem.IncomeGreen
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.model.Budget
import com.walletq.core.model.BudgetSpendingSummary
import com.walletq.core.model.Category
import com.walletq.core.model.Household
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.feature.activity.OwnershipFilter
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BudgetsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    BudgetsScreenContent(
        uiState = uiState,
        formState = formState,
        onBackClick = onBackClick,
        onOwnershipFilterSelected = viewModel::onOwnershipFilterSelected,
        onShowArchivedToggled = viewModel::onShowArchivedToggled,
        onCreateBudgetClick = viewModel::openCreateBudgetSheet,
        onEditBudgetClick = viewModel::openEditBudgetSheet,
        onArchiveBudgetClick = viewModel::archiveBudget,
        onUnarchiveBudgetClick = viewModel::unarchiveBudget,
        onNameChanged = viewModel::onNameChanged,
        onYearMonthChanged = viewModel::onYearMonthChanged,
        onCurrencyChanged = viewModel::onCurrencyChanged,
        onAllocatedAmountChanged = viewModel::onAllocatedAmountChanged,
        onOwnershipChanged = viewModel::onOwnershipChanged,
        onHouseholdSelected = viewModel::onHouseholdSelected,
        onAddCategoryAllocation = viewModel::addCategoryAllocation,
        onCategoryAmountChanged = viewModel::updateCategoryAllocationAmount,
        onRemoveCategoryAllocation = viewModel::removeCategoryAllocation,
        onSubmitForm = viewModel::submitBudgetForm,
        onDismissForm = viewModel::dismissFormSheet,
        onSnackbarConsumed = viewModel::consumeSnackbar,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreenContent(
    uiState: BudgetsUiState,
    formState: BudgetFormState,
    onBackClick: () -> Unit,
    onOwnershipFilterSelected: (OwnershipFilter) -> Unit,
    onShowArchivedToggled: (Boolean) -> Unit,
    onCreateBudgetClick: () -> Unit,
    onEditBudgetClick: (Budget) -> Unit,
    onArchiveBudgetClick: (String) -> Unit,
    onUnarchiveBudgetClick: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onYearMonthChanged: (YearMonth) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onAllocatedAmountChanged: (String) -> Unit,
    onOwnershipChanged: (OwnershipType) -> Unit,
    onHouseholdSelected: (String) -> Unit,
    onAddCategoryAllocation: (String) -> Unit,
    onCategoryAmountChanged: (String, String) -> Unit,
    onRemoveCategoryAllocation: (String) -> Unit,
    onSubmitForm: () -> Unit,
    onDismissForm: () -> Unit,
    onSnackbarConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onSnackbarConsumed()
        }
    }

    Scaffold(
        topBar = {
            WalletQTopBar(
                title = "Budgets",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onCreateBudgetClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Create Budget") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                WalletQEmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Unable to Load Budgets",
                    message = uiState.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.spaceMedium, vertical = Dimensions.spaceSmall),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                    ) {
                        OwnershipFilter.entries.forEach { ownership ->
                            FilterChip(
                                selected = uiState.filterState.ownershipFilter == ownership,
                                onClick = { onOwnershipFilterSelected(ownership) },
                                label = { Text(ownership.label) }
                            )
                        }
                        FilterChip(
                            selected = uiState.filterState.showArchived,
                            onClick = { onShowArchivedToggled(!uiState.filterState.showArchived) },
                            label = { Text("Show Archived") }
                        )
                    }

                    if (uiState.budgetSummaries.isEmpty()) {
                        WalletQEmptyState(
                            icon = Icons.Outlined.Savings,
                            title = "No Budgets Found",
                            message = "Create a monthly personal or household budget to track spending limits by currency and category.",
                            actionButton = {
                                Button(onClick = onCreateBudgetClick) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                                    Text("Create Budget")
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Dimensions.spaceMedium,
                                end = Dimensions.spaceMedium,
                                top = Dimensions.spaceSmall,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
                        ) {
                            items(
                                items = uiState.budgetSummaries,
                                key = { it.budget.id }
                            ) { summary ->
                                BudgetCard(
                                    summary = summary,
                                    household = summary.budget.householdId?.let { uiState.householdsById[it] },
                                    categoriesById = uiState.categoriesById,
                                    onEditClick = { onEditBudgetClick(summary.budget) },
                                    onArchiveClick = { onArchiveBudgetClick(summary.budget.id) },
                                    onUnarchiveClick = { onUnarchiveBudgetClick(summary.budget.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isFormSheetOpen) {
        AddEditBudgetSheet(
            formState = formState,
            expenseCategories = uiState.expenseCategories,
            categoriesById = uiState.categoriesById,
            households = uiState.households,
            onNameChanged = onNameChanged,
            onYearMonthChanged = onYearMonthChanged,
            onCurrencyChanged = onCurrencyChanged,
            onAllocatedAmountChanged = onAllocatedAmountChanged,
            onOwnershipChanged = onOwnershipChanged,
            onHouseholdSelected = onHouseholdSelected,
            onAddCategoryAllocation = onAddCategoryAllocation,
            onCategoryAmountChanged = onCategoryAmountChanged,
            onRemoveCategoryAllocation = onRemoveCategoryAllocation,
            onSubmit = onSubmitForm,
            onDismiss = onDismissForm
        )
    }
}

@Composable
private fun BudgetCard(
    summary: BudgetSpendingSummary,
    household: Household?,
    categoriesById: Map<String, Category>,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit
) {
    val budget = summary.budget
    val monthLabel = remember(budget.startDateEpochMs) {
        Instant.ofEpochMilli(budget.startDateEpochMs)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
    }
    val allocatedMoney = Money(summary.allocatedMinorUnits, budget.currency)
    val spentMoney = Money(summary.actualSpendingMinorUnits, budget.currency)
    val remainingMoney = Money(summary.remainingMinorUnits, budget.currency)
    val overMoney = Money(summary.overBudgetMinorUnits, budget.currency)
    val isOver = summary.overBudgetMinorUnits > 0L

    WalletQCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val scopeLabel = if (budget.ownershipType == OwnershipType.HOUSEHOLD) {
                    household?.name ?: "Household"
                } else {
                    "Personal"
                }
                Text(
                    text = "$monthLabel • $scopeLabel • ${budget.currency.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (budget.isArchived) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

        val progress = (summary.percentageConsumed.coerceIn(0, 100)) / 100f
        LinearProgressIndicator(
            progress = { progress },
            color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spent: ${spentMoney.format()}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Limit: ${allocatedMoney.format()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = if (isOver) {
                "Over budget by ${overMoney.format()} (${summary.percentageConsumed}%)"
            } else {
                "Remaining: ${remainingMoney.format()} (${summary.percentageConsumed}%)"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (isOver) ExpenseRed else IncomeGreen
        )

        if (summary.categorySummaries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
            summary.categorySummaries.forEach { catSum ->
                val catName = categoriesById[catSum.categoryId]?.name ?: catSum.categoryId
                val catSpent = Money(catSum.actualSpendingMinorUnits, budget.currency).format()
                val catAlloc = Money(catSum.allocatedMinorUnits, budget.currency).format()
                Text(
                    text = "• $catName: $catSpent / $catAlloc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.spaceXSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onEditClick) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
            if (budget.isArchived) {
                TextButton(onClick = onUnarchiveClick) {
                    Icon(Icons.Outlined.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            } else {
                TextButton(onClick = onArchiveClick) {
                    Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Archive")
                }
            }
        }
    }
}
