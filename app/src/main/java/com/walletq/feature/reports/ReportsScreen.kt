package com.walletq.feature.reports

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            WalletQTopBar(title = "Financial Reports")
        },
        modifier = modifier
    ) { innerPadding ->
        WalletQEmptyState(
            icon = Icons.Outlined.BarChart,
            title = "Reports & Analytics",
            message = "Visual cash flow analyses, budget comparisons, and spending category insights will be available in Phase 4.",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
