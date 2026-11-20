package com.walletq.feature.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            WalletQTopBar(title = "Financial Activity")
        },
        modifier = modifier
    ) { innerPadding ->
        WalletQEmptyState(
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = "No Transactions Recorded",
            message = "Income, expense, and transfer records will be managed here in Phase 2.",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
