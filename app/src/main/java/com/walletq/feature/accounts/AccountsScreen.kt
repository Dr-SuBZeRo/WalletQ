package com.walletq.feature.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.ExpenseRed
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.model.Account
import com.walletq.core.model.AccountType
import com.walletq.core.model.Household
import com.walletq.core.model.Money
import com.walletq.core.model.OwnershipType
import com.walletq.feature.accounts.components.AccountDetailsSheet
import com.walletq.feature.accounts.components.AccountVisuals
import com.walletq.feature.accounts.components.AddEditAccountSheet
import com.walletq.feature.accounts.components.AdjustBalanceSheet
import com.walletq.feature.activity.OwnershipFilter

@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accountFormState by viewModel.accountFormState.collectAsStateWithLifecycle()
    val balanceAdjustmentState by viewModel.balanceAdjustmentState.collectAsStateWithLifecycle()

    AccountsScreenContent(
        uiState = uiState,
        accountFormState = accountFormState,
        balanceAdjustmentState = balanceAdjustmentState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onOwnershipFilterSelected = viewModel::onOwnershipFilterSelected,
        onAccountTypeFilterSelected = viewModel::onAccountTypeFilterSelected,
        onShowArchivedToggled = viewModel::onShowArchivedToggled,
        onClearFilters = viewModel::clearFilters,
        onAddAccountClick = viewModel::openAddAccountSheet,
        onAccountClick = viewModel::openAccountDetails,
        onEditAccountClick = viewModel::openEditAccountSheet,
        onAdjustBalanceClick = viewModel::openAdjustBalanceSheet,
        onArchiveConfirm = viewModel::archiveAccount,
        onUnarchiveClick = viewModel::unarchiveAccount,
        onNameChanged = viewModel::onAccountNameChanged,
        onTypeChanged = viewModel::onAccountTypeChanged,
        onCurrencyChanged = viewModel::onAccountCurrencyChanged,
        onOwnershipChanged = viewModel::onAccountOwnershipChanged,
        onHouseholdSelected = viewModel::onAccountHouseholdSelected,
        onOpeningBalanceChanged = viewModel::onOpeningBalanceChanged,
        onOpeningBalanceDateChanged = viewModel::onOpeningBalanceDateChanged,
        onDescriptionChanged = viewModel::onAccountDescriptionChanged,
        onColorSelected = viewModel::onAccountColorSelected,
        onIconSelected = viewModel::onAccountIconSelected,
        onSubmitAccountForm = viewModel::submitAccountForm,
        onTargetBalanceChanged = viewModel::onTargetBalanceChanged,
        onAdjustmentReasonChanged = viewModel::onAdjustmentReasonChanged,
        onSubmitBalanceAdjustment = viewModel::submitBalanceAdjustment,
        onDismissSheet = viewModel::dismissSheet,
        onSnackbarConsumed = viewModel::consumeSnackbar,
        modifier = modifier
    )
}

@Composable
fun AccountsScreenContent(
    uiState: AccountsUiState,
    accountFormState: AccountFormState,
    balanceAdjustmentState: BalanceAdjustmentFormState,
    onSearchQueryChanged: (String) -> Unit,
    onOwnershipFilterSelected: (OwnershipFilter) -> Unit,
    onAccountTypeFilterSelected: (AccountType?) -> Unit,
    onShowArchivedToggled: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onEditAccountClick: (Account) -> Unit,
    onAdjustBalanceClick: (Account) -> Unit,
    onArchiveConfirm: (String) -> Unit,
    onUnarchiveClick: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (AccountType) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onOwnershipChanged: (OwnershipType) -> Unit,
    onHouseholdSelected: (String) -> Unit,
    onOpeningBalanceChanged: (String) -> Unit,
    onOpeningBalanceDateChanged: (Long) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onSubmitAccountForm: () -> Unit,
    onTargetBalanceChanged: (String) -> Unit,
    onAdjustmentReasonChanged: (String) -> Unit,
    onSubmitBalanceAdjustment: () -> Unit,
    onDismissSheet: () -> Unit,
    onSnackbarConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onSnackbarConsumed()
        }
    }

    val hasActiveFilters = uiState.filterState.searchQuery.isNotBlank() ||
        uiState.filterState.ownershipFilter != OwnershipFilter.ALL ||
        uiState.filterState.selectedType != null ||
        uiState.filterState.showArchived

    Scaffold(
        topBar = {
            WalletQTopBar(
                title = "Accounts & Wallets",
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Toggle account filters",
                            tint = if (hasActiveFilters) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onAddAccountClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Account") }
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
                    title = "Unable to Load Accounts",
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
                    AnimatedVisibility(visible = showFilters) {
                        AccountFilterPanel(
                            filterState = uiState.filterState,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onOwnershipFilterSelected = onOwnershipFilterSelected,
                            onAccountTypeFilterSelected = onAccountTypeFilterSelected,
                            onShowArchivedToggled = onShowArchivedToggled,
                            onClearFilters = onClearFilters
                        )
                    }

                    if (uiState.accounts.isEmpty()) {
                        WalletQEmptyState(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            title = if (hasActiveFilters) "No Matching Accounts" else "No Accounts Added",
                            message = if (hasActiveFilters) {
                                "No accounts match your current search or filter criteria."
                            } else {
                                "Add your cash wallets, bank accounts, savings, or credit cards to start tracking balances."
                            },
                            actionButton = {
                                if (hasActiveFilters) {
                                    TextButton(onClick = onClearFilters) {
                                        Text("Clear Filters")
                                    }
                                } else {
                                    Button(onClick = onAddAccountClick) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                                        Text("Add Account")
                                    }
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
                            if (uiState.currencySummaries.isNotEmpty()) {
                                item(key = "currency_summaries") {
                                    CurrencySummaryRow(summaries = uiState.currencySummaries)
                                }
                            }

                            if (uiState.personalAccounts.isNotEmpty()) {
                                item(key = "personal_header") {
                                    SectionHeader(
                                        title = "Personal Accounts",
                                        count = uiState.personalAccounts.size
                                    )
                                }
                                items(
                                    items = uiState.personalAccounts,
                                    key = { it.id }
                                ) { account ->
                                    AccountCard(
                                        account = account,
                                        household = null,
                                        onClick = { onAccountClick(account.id) }
                                    )
                                }
                            }

                            if (uiState.householdAccounts.isNotEmpty()) {
                                item(key = "household_header") {
                                    SectionHeader(
                                        title = "Household Accounts",
                                        count = uiState.householdAccounts.size
                                    )
                                }
                                items(
                                    items = uiState.householdAccounts,
                                    key = { it.id }
                                ) { account ->
                                    AccountCard(
                                        account = account,
                                        household = account.householdId?.let { uiState.householdsById[it] },
                                        onClick = { onAccountClick(account.id) }
                                    )
                                }
                            }

                            if (uiState.archivedAccounts.isNotEmpty()) {
                                item(key = "archived_header") {
                                    SectionHeader(
                                        title = "Archived Accounts",
                                        count = uiState.archivedAccounts.size
                                    )
                                }
                                items(
                                    items = uiState.archivedAccounts,
                                    key = { it.id }
                                ) { account ->
                                    AccountCard(
                                        account = account,
                                        household = account.householdId?.let { uiState.householdsById[it] },
                                        onClick = { onAccountClick(account.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (uiState.activeSheet) {
        ActiveAccountSheet.ADD_OR_EDIT -> {
            AddEditAccountSheet(
                formState = accountFormState,
                households = uiState.households,
                onNameChanged = onNameChanged,
                onTypeChanged = onTypeChanged,
                onCurrencyChanged = onCurrencyChanged,
                onOwnershipChanged = onOwnershipChanged,
                onHouseholdSelected = onHouseholdSelected,
                onOpeningBalanceChanged = onOpeningBalanceChanged,
                onOpeningBalanceDateChanged = onOpeningBalanceDateChanged,
                onDescriptionChanged = onDescriptionChanged,
                onColorSelected = onColorSelected,
                onIconSelected = onIconSelected,
                onSubmit = onSubmitAccountForm,
                onDismiss = onDismissSheet
            )
        }

        ActiveAccountSheet.DETAILS -> {
            val selected = uiState.selectedAccount
            if (selected != null) {
                AccountDetailsSheet(
                    account = selected,
                    household = selected.householdId?.let { uiState.householdsById[it] },
                    ledgerDerivedBalanceMinorUnits = uiState.selectedAccountLedgerBalanceMinorUnits,
                    transactions = uiState.selectedAccountTransactions,
                    onEditClick = { onEditAccountClick(selected) },
                    onAdjustBalanceClick = { onAdjustBalanceClick(selected) },
                    onArchiveConfirm = { onArchiveConfirm(selected.id) },
                    onUnarchiveClick = { onUnarchiveClick(selected.id) },
                    onDismiss = onDismissSheet
                )
            }
        }

        ActiveAccountSheet.ADJUST_BALANCE -> {
            AdjustBalanceSheet(
                state = balanceAdjustmentState,
                onTargetBalanceChanged = onTargetBalanceChanged,
                onReasonChanged = onAdjustmentReasonChanged,
                onSubmit = onSubmitBalanceAdjustment,
                onDismiss = onDismissSheet
            )
        }

        ActiveAccountSheet.NONE -> Unit
    }
}

@Composable
private fun CurrencySummaryRow(summaries: List<CurrencyBalanceSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        Text(
            text = "Total Balances by Currency",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            items(summaries, key = { it.currency.code }) { summary ->
                val money = Money(summary.totalMinorUnits, summary.currency)
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Dimensions.spaceMedium,
                            vertical = Dimensions.spaceSmall
                        )
                    ) {
                        Text(
                            text = "${summary.currency.code} • ${summary.accountCount} account(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = money.format(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccountCard(
    account: Account,
    household: Household?,
    onClick: () -> Unit
) {
    val accentColor = AccountVisuals.resolveColor(
        account.colorIdentifier,
        MaterialTheme.colorScheme.primary
    )
    val balance = Money(account.currentBalanceMinorUnits, account.currency)

    WalletQCard(
        modifier = Modifier
            .alpha(if (account.isArchived) 0.65f else 1f)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AccountVisuals.resolveIcon(account.type, account.iconIdentifier),
                    contentDescription = null,
                    tint = accentColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val ownershipText = if (account.ownershipType == OwnershipType.HOUSEHOLD) {
                    household?.name ?: "Household"
                } else {
                    "Personal"
                }
                Text(
                    text = "${account.type.displayName} • $ownershipText • ${account.currency.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = balance.format(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance.isNegative()) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
                if (account.isArchived) {
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountFilterPanel(
    filterState: AccountFilterState,
    onSearchQueryChanged: (String) -> Unit,
    onOwnershipFilterSelected: (OwnershipFilter) -> Unit,
    onAccountTypeFilterSelected: (AccountType?) -> Unit,
    onShowArchivedToggled: (Boolean) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.spaceMedium, vertical = Dimensions.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
    ) {
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search accounts, currency, type...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            OwnershipFilter.entries.forEach { ownership ->
                FilterChip(
                    selected = filterState.ownershipFilter == ownership,
                    onClick = { onOwnershipFilterSelected(ownership) },
                    label = { Text(ownership.label) }
                )
            }
            FilterChip(
                selected = filterState.showArchived,
                onClick = { onShowArchivedToggled(!filterState.showArchived) },
                label = { Text("Show Archived") }
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            FilterChip(
                selected = filterState.selectedType == null,
                onClick = { onAccountTypeFilterSelected(null) },
                label = { Text("All Types") }
            )
            AccountVisuals.selectableAccountTypes.forEach { type ->
                FilterChip(
                    selected = filterState.selectedType == type,
                    onClick = {
                        onAccountTypeFilterSelected(
                            if (filterState.selectedType == type) null else type
                        )
                    },
                    label = { Text(type.displayName) }
                )
            }
        }

        if (filterState.searchQuery.isNotBlank() ||
            filterState.ownershipFilter != OwnershipFilter.ALL ||
            filterState.selectedType != null ||
            filterState.showArchived
        ) {
            TextButton(onClick = onClearFilters) {
                Text("Reset Filters")
            }
        }
    }
}
