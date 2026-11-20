package com.walletq.feature.accounts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar

@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            WalletQTopBar(title = "Accounts & Wallets")
        },
        modifier = modifier
    ) { innerPadding ->
        WalletQEmptyState(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "No Accounts Added",
            message = "Cash wallets, bank accounts, savings, and credit cards across personal and household profiles will be configured in Phase 3.",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
