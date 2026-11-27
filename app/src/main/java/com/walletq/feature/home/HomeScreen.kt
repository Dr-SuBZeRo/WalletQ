package com.walletq.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.VibrantCoral
import com.walletq.core.designsystem.VibrantElectricBlue
import com.walletq.core.designsystem.VibrantEmerald
import com.walletq.core.designsystem.VibrantPurple
import com.walletq.core.designsystem.WalletQMonetaryTypography
import com.walletq.core.designsystem.WalletQMotion
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.WalletQVibrantPalette
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQFilterChip
import com.walletq.core.designsystem.component.WalletQLoadingState
import com.walletq.core.designsystem.component.WalletQProgressIndicator
import com.walletq.core.designsystem.component.WalletQSectionHeader
import com.walletq.core.designsystem.component.WalletQStatusBadge
import com.walletq.core.designsystem.component.WalletQStatusTone
import com.walletq.core.designsystem.component.WalletQTransactionRow
import com.walletq.core.designsystem.rememberReducedMotion
import com.walletq.core.model.Account
import com.walletq.core.model.BudgetSpendingSummary
import com.walletq.core.model.Money
import com.walletq.core.model.Transaction
import com.walletq.core.model.TransactionStatus
import com.walletq.core.model.TransactionType
import com.walletq.feature.accounts.components.AccountVisuals
import com.walletq.feature.activity.OwnershipFilter
import com.walletq.feature.activity.categoryIconFor
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onOwnershipFilterSelected = viewModel::onOwnershipFilterSelected,
        onToggleAmountVisibility = viewModel::onToggleAmountVisibility,
        onNavigateToAccounts = onNavigateToAccounts,
        onNavigateToActivity = onNavigateToActivity,
        onNavigateToBudgets = onNavigateToBudgets,
        onQuickExpense = onQuickExpense,
        onQuickIncome = onQuickIncome,
        onQuickTransfer = onQuickTransfer,
        onQuickAddAccount = onQuickAddAccount,
        modifier = modifier
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOwnershipFilterSelected: (OwnershipFilter) -> Unit,
    onToggleAmountVisibility: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = WalletQThemeTokens.colors
    val reducedMotion = rememberReducedMotion()
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) contentVisible = true
    }

    Scaffold(
        containerColor = tokens.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                WalletQLoadingState(
                    message = "Loading your financial overview...",
                    modifier = Modifier.padding(innerPadding)
                )
            }

            uiState.errorMessage != null -> {
                WalletQEmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Unable to Load Dashboard",
                    message = uiState.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = WalletQMotion.contentEnter(reducedMotion)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars),
                        contentPadding = PaddingValues(
                            start = Dimensions.screenHorizontalPadding,
                            end = Dimensions.screenHorizontalPadding,
                            top = Dimensions.spaceMedium,
                            bottom = Dimensions.spaceXXLarge
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
                    ) {
                        // 1. Header: Greeting, Month Selector, Ownership Selector, Visibility Toggle
                        item(key = "header") {
                            HomeHeader(
                                uiState = uiState,
                                onPreviousMonth = onPreviousMonth,
                                onNextMonth = onNextMonth,
                                onOwnershipFilterSelected = onOwnershipFilterSelected,
                                onToggleAmountVisibility = onToggleAmountVisibility
                            )
                        }

                        // 2. Net Worth Hero Card
                        item(key = "net_worth") {
                            NetWorthHeroCard(
                                uiState = uiState,
                                onToggleAmountVisibility = onToggleAmountVisibility,
                                reducedMotion = reducedMotion
                            )
                        }

                        // 3. Vibrant Income & Expense Summary Cards
                        item(key = "income_expense") {
                            IncomeExpenseRow(
                                uiState = uiState,
                                reducedMotion = reducedMotion
                            )
                        }

                        // 4. Net Cash Flow Card
                        item(key = "cash_flow") {
                            CashFlowCard(
                                uiState = uiState,
                                reducedMotion = reducedMotion
                            )
                        }

                        // 5. Quick Actions
                        item(key = "quick_actions") {
                            QuickActionsRow(
                                onQuickExpense = onQuickExpense,
                                onQuickIncome = onQuickIncome,
                                onQuickTransfer = onQuickTransfer,
                                onQuickAddAccount = onQuickAddAccount
                            )
                        }

                        // 6. Account Overview
                        item(key = "accounts_overview") {
                            AccountOverviewSection(
                                accounts = uiState.accounts,
                                isHidden = uiState.isAmountsHidden,
                                onViewAllAccounts = onNavigateToAccounts
                            )
                        }

                        // 7. Budget Overview
                        item(key = "budget_overview") {
                            BudgetOverviewSection(
                                budgets = uiState.activeBudgets,
                                isHidden = uiState.isAmountsHidden,
                                onNavigateToBudgets = onNavigateToBudgets
                            )
                        }

                        // 8. Recent Transactions
                        item(key = "recent_transactions_header") {
                            WalletQSectionHeader(
                                title = "Recent Activity",
                                action = {
                                    TextButton(onClick = onNavigateToActivity) {
                                        Text("View All")
                                    }
                                }
                            )
                        }

                        if (uiState.recentTransactions.isEmpty()) {
                            item(key = "recent_transactions_empty") {
                                WalletQCard {
                                    WalletQEmptyState(
                                        icon = Icons.Outlined.AccountBalanceWallet,
                                        title = "No Transactions Yet",
                                        message = "Record an expense, income, or transfer to see your recent activity here."
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.recentTransactions,
                                key = { it.id }
                            ) { tx ->
                                RecentTransactionItem(
                                    transaction = tx,
                                    accountName = uiState.accountsById[tx.accountId]?.name ?: "Account",
                                    categoryName = tx.categoryId?.let { uiState.categoriesById[it]?.name },
                                    categoryIconId = tx.categoryId?.let { uiState.categoriesById[it]?.iconIdentifier },
                                    isHidden = uiState.isAmountsHidden,
                                    onClick = onNavigateToActivity
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    uiState: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOwnershipFilterSelected: (OwnershipFilter) -> Unit,
    onToggleAmountVisibility: () -> Unit
) {
    val tokens = WalletQThemeTokens.colors
    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting, ${uiState.profileName}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Financial Overview",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.secondaryText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = tokens.surface
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronLeft,
                                contentDescription = "Previous month",
                                tint = tokens.secondaryText,
                                modifier = Modifier.size(Dimensions.iconSmall)
                            )
                        }
                        Text(
                            text = uiState.selectedMonth.format(monthFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.primaryText
                        )
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Next month",
                                tint = tokens.secondaryText,
                                modifier = Modifier.size(Dimensions.iconSmall)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(Dimensions.spaceXSmall))
                IconButton(onClick = onToggleAmountVisibility) {
                    Icon(
                        imageVector = if (uiState.isAmountsHidden) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (uiState.isAmountsHidden) "Show amounts" else "Hide amounts",
                        tint = tokens.secondaryText
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
            OwnershipFilter.entries.forEach { filter ->
                WalletQFilterChip(
                    selected = uiState.ownershipFilter == filter,
                    label = filter.label,
                    onClick = { onOwnershipFilterSelected(filter) }
                )
            }
        }
    }
}

@Composable
private fun NetWorthHeroCard(
    uiState: HomeUiState,
    onToggleAmountVisibility: () -> Unit,
    reducedMotion: Boolean
) {
    val netWorthFormatted = if (uiState.isAmountsHidden) {
        "${uiState.reportingCurrency.symbol} ••••••"
    } else {
        Money(uiState.netWorthMinorUnits, uiState.reportingCurrency).format()
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        WalletQVibrantPalette.netWorthGradientStart,
                        WalletQVibrantPalette.netWorthGradientEnd
                    )
                )
            )
            .clickable(onClick = onToggleAmountVisibility)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Worth (${uiState.reportingCurrency.code})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFB7C5E3)
                )
                WalletQStatusBadge(
                    text = uiState.ownershipFilter.label,
                    tone = WalletQStatusTone.INFO
                )
            }

            AnimatedContent(
                targetState = netWorthFormatted,
                transitionSpec = WalletQMotion.discreteValueTransition(reducedMotion),
                label = "net_worth_value"
            ) { value ->
                Text(
                    text = value,
                    style = WalletQMonetaryTypography.hero,
                    color = Color.White
                )
            }

            if (uiState.otherCurrencyAccountCount > 0) {
                Text(
                    text = "+ ${uiState.otherCurrencyAccountCount} account(s) in other currencies (kept separate)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9FB0D4)
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseRow(
    uiState: HomeUiState,
    reducedMotion: Boolean
) {
    val incomeText = if (uiState.isAmountsHidden) {
        "${uiState.reportingCurrency.symbol} ••••"
    } else {
        Money(uiState.monthlyIncomeMinorUnits, uiState.reportingCurrency).format()
    }
    val expenseText = if (uiState.isAmountsHidden) {
        "${uiState.reportingCurrency.symbol} ••••"
    } else {
        Money(uiState.monthlyExpenseMinorUnits, uiState.reportingCurrency).format()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMediumSmall)
    ) {
        GradientStatCard(
            title = "Income",
            amount = incomeText,
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            gradientColors = listOf(
                WalletQVibrantPalette.incomeGradientStart,
                WalletQVibrantPalette.incomeGradientEnd
            ),
            contentColor = WalletQVibrantPalette.incomeContent,
            reducedMotion = reducedMotion,
            modifier = Modifier.weight(1f)
        )
        GradientStatCard(
            title = "Expenses",
            amount = expenseText,
            icon = Icons.AutoMirrored.Outlined.TrendingDown,
            gradientColors = listOf(
                WalletQVibrantPalette.expenseGradientStart,
                WalletQVibrantPalette.expenseGradientEnd
            ),
            contentColor = WalletQVibrantPalette.expenseContent,
            reducedMotion = reducedMotion,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GradientStatCard(
    title: String,
    amount: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    contentColor: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(colors = gradientColors))
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.85f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(Dimensions.iconMedium)
                )
            }
            AnimatedContent(
                targetState = amount,
                transitionSpec = WalletQMotion.discreteValueTransition(reducedMotion),
                label = "stat_amount_$title"
            ) { value ->
                Text(
                    text = value,
                    style = WalletQMonetaryTypography.large,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CashFlowCard(
    uiState: HomeUiState,
    reducedMotion: Boolean
) {
    val tokens = WalletQThemeTokens.colors
    val isPositive = uiState.netCashFlowMinorUnits >= 0L
    val flowMoney = Money(uiState.netCashFlowMinorUnits, uiState.reportingCurrency)
    val formattedFlow = if (uiState.isAmountsHidden) {
        "${uiState.reportingCurrency.symbol} ••••"
    } else {
        (if (uiState.netCashFlowMinorUnits > 0L) "+" else "") + flowMoney.format()
    }

    WalletQCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Monthly Cash Flow",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.secondaryText
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceXSmall))
                AnimatedContent(
                    targetState = formattedFlow,
                    transitionSpec = WalletQMotion.discreteValueTransition(reducedMotion),
                    label = "cash_flow_value"
                ) { value ->
                    Text(
                        text = value,
                        style = WalletQMonetaryTypography.large,
                        color = if (isPositive) tokens.success else tokens.error
                    )
                }
            }
            WalletQStatusBadge(
                text = if (isPositive) "Surplus" else "Deficit",
                tone = if (isPositive) WalletQStatusTone.SUCCESS else WalletQStatusTone.ERROR
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickAddAccount: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        WalletQSectionHeader(title = "Quick Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
        ) {
            QuickActionButton(
                label = "Expense",
                icon = Icons.AutoMirrored.Outlined.TrendingDown,
                accentColor = VibrantCoral,
                onClick = onQuickExpense,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Income",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                accentColor = VibrantEmerald,
                onClick = onQuickIncome,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Transfer",
                icon = Icons.Outlined.SwapHoriz,
                accentColor = VibrantElectricBlue,
                onClick = onQuickTransfer,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Account",
                icon = Icons.Outlined.Add,
                accentColor = VibrantPurple,
                onClick = onQuickAddAccount,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = WalletQThemeTokens.colors
    WalletQCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceXSmall)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(Dimensions.iconSmall)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccountOverviewSection(
    accounts: List<Account>,
    isHidden: Boolean,
    onViewAllAccounts: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        WalletQSectionHeader(
            title = "Accounts",
            action = {
                TextButton(onClick = onViewAllAccounts) {
                    Text("All Accounts")
                }
            }
        )
        if (accounts.isEmpty()) {
            WalletQCard(onClick = onViewAllAccounts) {
                Text(
                    text = "No active accounts yet. Tap to add your first wallet or bank account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalletQThemeTokens.colors.secondaryText
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMediumSmall)
            ) {
                items(accounts, key = { it.id }) { account ->
                    val theme = WalletQVibrantPalette.resolveAccountTheme(account.colorIdentifier, account.id)
                    val balanceText = if (isHidden) {
                        "${account.currency.symbol} ••••"
                    } else {
                        Money(account.currentBalanceMinorUnits, account.currency).format()
                    }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent,
                        modifier = Modifier
                            .width(172.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(theme.startColor, theme.endColor)
                                )
                            )
                            .clickable(onClick = onViewAllAccounts)
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimensions.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AccountVisuals.resolveIcon(account.type, account.iconIdentifier),
                                    contentDescription = null,
                                    tint = theme.contentColor,
                                    modifier = Modifier.size(Dimensions.iconMedium)
                                )
                                Text(
                                    text = account.currency.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.contentColor.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = theme.contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = balanceText,
                                style = WalletQMonetaryTypography.medium,
                                color = theme.contentColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetOverviewSection(
    budgets: List<BudgetSpendingSummary>,
    isHidden: Boolean,
    onNavigateToBudgets: () -> Unit
) {
    val tokens = WalletQThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        WalletQSectionHeader(
            title = "Monthly Budgets",
            action = {
                TextButton(onClick = onNavigateToBudgets) {
                    Text(if (budgets.isEmpty()) "Create" else "Manage")
                }
            }
        )
        if (budgets.isEmpty()) {
            WalletQCard(onClick = onNavigateToBudgets) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimensions.iconBadgeContainer)
                            .clip(CircleShape)
                            .background(tokens.elevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No Active Budget This Month",
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.primaryText
                        )
                        Text(
                            text = "Tap to set a monthly spending limit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.secondaryText
                        )
                    }
                }
            }
        } else {
            budgets.take(2).forEach { summary ->
                val budget = summary.budget
                val isOver = summary.overBudgetMinorUnits > 0L
                val spentStr = if (isHidden) "${budget.currency.symbol} ••••" else Money(summary.actualSpendingMinorUnits, budget.currency).format()
                val allocStr = if (isHidden) "${budget.currency.symbol} ••••" else Money(summary.allocatedMinorUnits, budget.currency).format()
                val remStr = if (isHidden) "${budget.currency.symbol} ••••" else Money(summary.remainingMinorUnits, budget.currency).format()

                WalletQCard(onClick = onNavigateToBudgets) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = budget.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.primaryText
                        )
                        WalletQStatusBadge(
                            text = "${summary.percentageConsumed}%",
                            tone = if (isOver) WalletQStatusTone.ERROR else WalletQStatusTone.SUCCESS
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    WalletQProgressIndicator(
                        progress = (summary.percentageConsumed.coerceIn(0, 100)) / 100f,
                        tone = if (isOver) WalletQStatusTone.ERROR else WalletQStatusTone.INFO
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: $spentStr / $allocStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.secondaryText
                        )
                        Text(
                            text = if (isOver) "Over budget" else "Left: $remStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOver) tokens.error else tokens.success
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: Transaction,
    accountName: String,
    categoryName: String?,
    categoryIconId: String?,
    isHidden: Boolean,
    onClick: () -> Unit
) {
    val isReversed = transaction.status == TransactionStatus.REVERSED
    val tone = when (transaction.type) {
        TransactionType.INCOME -> WalletQStatusTone.SUCCESS
        TransactionType.EXPENSE -> WalletQStatusTone.ERROR
        TransactionType.TRANSFER -> WalletQStatusTone.INFO
    }
    val prefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }
    val amountStr = if (isHidden) {
        "${transaction.amount.currency.symbol} ••••"
    } else {
        "$prefix${transaction.amount.format()}"
    }

    val subtitleParts = buildList {
        add(accountName)
        if (categoryName != null) add(categoryName)
        if (transaction.type == TransactionType.TRANSFER) add("Transfer")
        if (isReversed) add("Reversed")
        add(AccountVisuals.formatDate(transaction.transactionDateEpochMs))
    }

    WalletQTransactionRow(
        title = transaction.description,
        subtitle = subtitleParts.joinToString(" • "),
        formattedAmount = amountStr,
        icon = if (transaction.type == TransactionType.TRANSFER) {
            Icons.Outlined.SwapHoriz
        } else {
            categoryIconFor(categoryIconId ?: "")
        },
        amountTone = tone,
        isReversed = isReversed,
        onClick = onClick
    )
}
