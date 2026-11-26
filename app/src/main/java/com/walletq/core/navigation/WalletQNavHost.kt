package com.walletq.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.walletq.core.model.ThemeMode
import com.walletq.core.model.UserPreferences
import com.walletq.feature.accounts.AccountsScreen
import com.walletq.feature.activity.ActivityScreen
import com.walletq.feature.budgets.BudgetsScreen
import com.walletq.feature.home.HomeScreen
import com.walletq.feature.more.MoreScreen
import com.walletq.feature.reports.ReportsScreen

private const val ROUTE_BUDGETS = "budgets"

@Composable
fun WalletQNavHost(
    navController: NavHostController,
    userPreferences: UserPreferences,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onDefaultCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.START_DESTINATION.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(defaultCurrencyCode = userPreferences.defaultCurrencyCode)
        }
        composable(TopLevelDestination.ACTIVITY.route) {
            ActivityScreen()
        }
        composable(TopLevelDestination.ACCOUNTS.route) {
            AccountsScreen()
        }
        composable(TopLevelDestination.REPORTS.route) {
            ReportsScreen()
        }
        composable(TopLevelDestination.MORE.route) {
            MoreScreen(
                userPreferences = userPreferences,
                onThemeModeChanged = onThemeModeChanged,
                onDynamicColorChanged = onDynamicColorChanged,
                onDefaultCurrencyChanged = onDefaultCurrencyChanged,
                onNavigateToBudgets = { navController.navigate(ROUTE_BUDGETS) }
            )
        }
        composable(ROUTE_BUDGETS) {
            BudgetsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
