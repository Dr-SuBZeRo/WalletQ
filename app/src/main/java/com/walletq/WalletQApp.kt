package com.walletq

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.walletq.core.designsystem.WalletQThemeTokens
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = tokens.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                WalletQBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
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
                )
            }
        }
    ) { innerPadding ->
        WalletQNavHost(
            navController = navController,
            userPreferences = userPreferences,
            onThemeModeChanged = viewModel::setThemeMode,
            onDynamicColorChanged = viewModel::setDynamicColor,
            onDefaultCurrencyChanged = viewModel::setDefaultCurrency,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
