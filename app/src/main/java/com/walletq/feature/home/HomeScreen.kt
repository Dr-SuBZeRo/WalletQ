package com.walletq.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQEmptyState
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.model.Currency
import com.walletq.core.model.Money

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    defaultCurrencyCode: String,
    modifier: Modifier = Modifier
) {
    val currency = Currency.findByCode(defaultCurrencyCode)
    val zeroBalance = Money.zero(currency).format()

    Scaffold(
        topBar = {
            WalletQTopBar(title = "WalletQ")
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimensions.spaceMedium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            // Net Worth Summary Card
            WalletQCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Net Worth",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceXSmall))
                        Text(
                            text = zeroBalance,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Profile: Personal") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Currency: ${currency.code}") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Architecture Pillars Card
            WalletQCard {
                Text(
                    text = "Foundation Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                ArchitecturePillarRow(title = "100% Offline-First", desc = "Zero mandatory cloud connectivity.")
                ArchitecturePillarRow(title = "No Trackers or SDKs", desc = "Zero telemetry, analytics, or ads.")
                ArchitecturePillarRow(title = "Strict Data Integrity", desc = "Integer minor units for financial math.")
            }

            // Clean Empty State
            WalletQEmptyState(
                icon = Icons.Outlined.AccountBalanceWallet,
                title = "No Recent Activity",
                message = "Your financial transactions and account balances will appear here once configured."
            )

            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
        }
    }
}

@Composable
private fun ArchitecturePillarRow(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.spaceXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.padding(start = Dimensions.spaceSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
