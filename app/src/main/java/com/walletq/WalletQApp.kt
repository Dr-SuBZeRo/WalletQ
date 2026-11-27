package com.walletq

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.VibrantCoral
import com.walletq.core.designsystem.VibrantElectricBlue
import com.walletq.core.designsystem.VibrantEmerald
import com.walletq.core.designsystem.VibrantPurple
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.component.WalletQBottomSheet
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.model.UserPreferences
import com.walletq.core.navigation.TopLevelDestination
import com.walletq.core.navigation.WalletQBottomBar
import com.walletq.core.navigation.WalletQNavHost

@Composable
fun WalletQApp(
    userPreferences: UserPreferences,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }
    val tokens = WalletQThemeTokens.colors

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var pendingActivityAction by remember { mutableStateOf<String?>(null) }
    var pendingAddAccount by remember { mutableStateOf(false) }

    fun navigateTopLevel(destination: TopLevelDestination) {
        if (currentRoute != destination.route) {
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = tokens.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                WalletQBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = ::navigateTopLevel,
                    onAddClick = { showQuickAddSheet = true }
                )
            }
        }
    ) { innerPadding ->
        WalletQNavHost(
            navController = navController,
            userPreferences = userPreferences,
            pendingActivityAction = pendingActivityAction,
            onActivityActionConsumed = { pendingActivityAction = null },
            pendingAddAccount = pendingAddAccount,
            onAddAccountConsumed = { pendingAddAccount = false },
            onNavigateToDestination = ::navigateTopLevel,
            onLaunchActivityAction = { action ->
                pendingActivityAction = action
                navigateTopLevel(TopLevelDestination.ACTIVITY)
            },
            onLaunchAddAccount = {
                pendingAddAccount = true
                navigateTopLevel(TopLevelDestination.ACCOUNTS)
            },
            onThemeModeChanged = viewModel::setThemeMode,
            onDynamicColorChanged = viewModel::setDynamicColor,
            onDefaultCurrencyChanged = viewModel::setDefaultCurrency,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showQuickAddSheet) {
        WalletQBottomSheet(
            title = "Quick Action",
            onDismiss = { showQuickAddSheet = false }
        ) {
            QuickAddActionOption(
                title = "Add Expense",
                subtitle = "Record a purchase or outgoing payment",
                icon = Icons.AutoMirrored.Outlined.TrendingDown,
                accentColor = VibrantCoral,
                onClick = {
                    showQuickAddSheet = false
                    pendingActivityAction = "EXPENSE"
                    navigateTopLevel(TopLevelDestination.ACTIVITY)
                }
            )
            QuickAddActionOption(
                title = "Add Income",
                subtitle = "Record salary, deposit, or inflow",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                accentColor = VibrantEmerald,
                onClick = {
                    showQuickAddSheet = false
                    pendingActivityAction = "INCOME"
                    navigateTopLevel(TopLevelDestination.ACTIVITY)
                }
            )
            QuickAddActionOption(
                title = "Transfer Funds",
                subtitle = "Move money between accounts",
                icon = Icons.Outlined.SwapHoriz,
                accentColor = VibrantElectricBlue,
                onClick = {
                    showQuickAddSheet = false
                    pendingActivityAction = "TRANSFER"
                    navigateTopLevel(TopLevelDestination.ACTIVITY)
                }
            )
            QuickAddActionOption(
                title = "New Account",
                subtitle = "Create a cash, bank, or credit card account",
                icon = Icons.Outlined.AccountBalanceWallet,
                accentColor = VibrantPurple,
                onClick = {
                    showQuickAddSheet = false
                    pendingAddAccount = true
                    navigateTopLevel(TopLevelDestination.ACCOUNTS)
                }
            )
        }
    }
}

@Composable
private fun QuickAddActionOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val tokens = WalletQThemeTokens.colors
    WalletQCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.iconBadgeContainer)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(Dimensions.iconMedium)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.primaryText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.secondaryText
                )
            }
        }
    }
}
