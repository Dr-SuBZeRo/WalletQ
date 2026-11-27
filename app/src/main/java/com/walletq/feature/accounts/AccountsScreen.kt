package com.walletq.feature.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQMonetaryTypography
import com.walletq.core.designsystem.WalletQMotion
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.WalletQVibrantPalette
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQFilterChip
import com.walletq.core.designsystem.component.WalletQLoadingState
import com.walletq.core.designsystem.component.WalletQSectionHeader
import com.walletq.core.designsystem.component.WalletQTextField
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.designsystem.rememberReducedMotion
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
    openAddOnStart: Boolean = false,
    onOpenAddConsumed: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accountFormState by viewModel.accountFormState.collectAsStateWithLifecycle()
    val balanceAdjustmentState by viewModel.balanceAdjustmentState.collectAsStateWithLifecycle()

    LaunchedEffect(openAddOnStart) {
        if (openAddOnStart) {
            viewModel.openAddAccountSheet()
            onOpenAddConsumed()
        }
    }

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
    val tokens = WalletQThemeTokens.colors
    val reducedMotion = rememberReducedMotion()
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
        containerColor = tokens.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                tokens.secondaryText
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Account") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                WalletQLoadingState(
                    message = "Loading accounts...",
                    modifier = Modifier.padding(innerPadding)
                )
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
                    AnimatedVisibility(
                        visible = showFilters,
                        enter = WalletQMotion.contentEnter(reducedMotion),
                        exit = WalletQMotion.contentExit(reducedMotion)
                    ) {
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
                                start = Dimensions.screenHorizontalPadding,
                                end = Dimensions.screenHorizontalPadding,
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
                                    WalletQSectionHeader(
                                        title = "Personal Accounts",
                                        trailingText = "${uiState.personalAccounts.size}"
                                    )
                                }
                                items(
                                    items = uiState.personalAccounts,
                                    key = { it.id }
                                ) { account ->
                                    VibrantAccountCard(
                                        account = account,
                                        household = null,
                                        monthlySummary = uiState.monthlySummariesByAccountId[account.id],
                                        reducedMotion = reducedMotion,
                                        onClick = { onAccountClick(account.id) }
                                    )
                                }
                            }

                            if (uiState.householdAccounts.isNotEmpty()) {
                                item(key = "household_header") {
                                    WalletQSectionHeader(
                                        title = "Household Accounts",
                                        trailingText = "${uiState.householdAccounts.size}"
                                    )
                                }
                                items(
                                    items = uiState.householdAccounts,
                                    key = { it.id }
                                ) { account ->
                                    VibrantAccountCard(
                                        account = account,
                                        household = account.householdId?.let { uiState.householdsById[it] },
                                        monthlySummary = uiState.monthlySummariesByAccountId[account.id],
                                        reducedMotion = reducedMotion,
                                        onClick = { onAccountClick(account.id) }
                                    )
                                }
                            }

                            if (uiState.archivedAccounts.isNotEmpty()) {
                                item(key = "archived_header") {
                                    WalletQSectionHeader(
                                        title = "Archived Accounts",
                                        trailingText = "${uiState.archivedAccounts.size}"
                                    )
                                }
                                items(
                                    items = uiState.archivedAccounts,
                                    key = { it.id }
                                ) { account ->
                                    VibrantAccountCard(
                                        account = account,
                                        household = account.householdId?.let { uiState.householdsById[it] },
                                        monthlySummary = uiState.monthlySummariesByAccountId[account.id],
                                        reducedMotion = reducedMotion,
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
                    reconciliationReport = uiState.selectedAccountReconciliation,
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
    val tokens = WalletQThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        Text(
            text = "Total Balances by Currency",
            style = MaterialTheme.typography.labelLarge,
            color = tokens.secondaryText
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            items(summaries, key = { it.currency.code }) { summary ->
                val money = Money(summary.totalMinorUnits, summary.currency)
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color.Transparent,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    WalletQVibrantPalette.netWorthGradientStart,
                                    WalletQVibrantPalette.netWorthGradientEnd
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Dimensions.spaceMedium,
                            vertical = Dimensions.spaceMediumSmall
                        )
                    ) {
                        Text(
                            text = "${summary.currency.code} • ${summary.accountCount} account(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB7C5E3)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = money.format(),
                            style = WalletQMonetaryTypography.large,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VibrantAccountCard(
    account: Account,
    household: Household?,
    monthlySummary: AccountMonthlySummary?,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val theme = WalletQVibrantPalette.resolveAccountTheme(account.colorIdentifier, account.id)
    val balance = Money(account.currentBalanceMinorUnits, account.currency)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.985f else 1f,
        animationSpec = WalletQMotion.fastSpec(reducedMotion),
        label = "account_card_scale"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (account.isArchived) 0.6f else 1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.startColor, theme.endColor)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMediumSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimensions.iconBadgeContainer)
                            .clip(CircleShape)
                            .background(theme.contentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AccountVisuals.resolveIcon(account.type, account.iconIdentifier),
                            contentDescription = null,
                            tint = theme.contentColor,
                            modifier = Modifier.size(Dimensions.iconMedium)
                        )
                    }
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.contentColor
                        )
                        val ownershipText = if (account.ownershipType == OwnershipType.HOUSEHOLD) {
                            household?.name ?: "Household"
                        } else {
                            "Personal"
                        }
                        Text(
                            text = "${account.type.displayName} • $ownershipText",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.contentColor.copy(alpha = 0.82f)
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = theme.contentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (account.isArchived) "Archived" else account.currency.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Large readable balance
            Text(
                text = balance.format(),
                style = WalletQMonetaryTypography.large,
                color = theme.contentColor
            )

            // Credit Card Liability Presentation
            if (account.type == AccountType.CREDIT_CARD && balance.isNegative()) {
                Text(
                    text = "Outstanding Liability: ${(-balance).format()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.contentColor.copy(alpha = 0.9f)
                )
            }

            // Monthly Income & Expense summaries per account when real data exists
            if (monthlySummary != null &&
                (monthlySummary.incomeMinorUnits > 0L || monthlySummary.expenseMinorUnits > 0L)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
                ) {
                    if (monthlySummary.incomeMinorUnits > 0L) {
                        val inc = Money(monthlySummary.incomeMinorUnits, account.currency).format()
                        Text(
                            text = "This month +$inc",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.contentColor.copy(alpha = 0.88f)
                        )
                    }
                    if (monthlySummary.expenseMinorUnits > 0L) {
                        val exp = Money(monthlySummary.expenseMinorUnits, account.currency).format()
                        Text(
                            text = "Spent -$exp",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.contentColor.copy(alpha = 0.88f)
                        )
                    }
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
            .padding(horizontal = Dimensions.screenHorizontalPadding, vertical = Dimensions.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
    ) {
        WalletQTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchQueryChanged,
            label = "Search",
            placeholder = "Search accounts, currency, type...",
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                }
            }
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
            OwnershipFilter.entries.forEach { ownership ->
                WalletQFilterChip(
                    selected = filterState.ownershipFilter == ownership,
                    label = ownership.label,
                    onClick = { onOwnershipFilterSelected(ownership) }
                )
            }
            WalletQFilterChip(
                selected = filterState.showArchived,
                label = "Show Archived",
                onClick = { onShowArchivedToggled(!filterState.showArchived) }
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
            WalletQFilterChip(
                selected = filterState.selectedType == null,
                label = "All Types",
                onClick = { onAccountTypeFilterSelected(null) }
            )
            AccountVisuals.selectableAccountTypes.forEach { type ->
                WalletQFilterChip(
                    selected = filterState.selectedType == type,
                    label = type.displayName,
                    onClick = {
                        onAccountTypeFilterSelected(
                            if (filterState.selectedType == type) null else type
                        )
                    }
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
