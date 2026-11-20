package com.walletq.feature.more

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.component.WalletQCard
import com.walletq.core.designsystem.component.WalletQTopBar
import com.walletq.core.model.Currency
import com.walletq.core.model.ThemeMode
import com.walletq.core.model.UserPreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
    userPreferences: UserPreferences,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onDefaultCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            WalletQTopBar(title = "Settings & More")
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
            // App Branding & Attribution
            WalletQCard {
                Text(
                    text = "WalletQ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Developed by SuBZeRo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Text(
                    text = "Free and open-source, privacy-focused personal and household money management.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Text(
                    text = "License: Apache License 2.0 • Version 1.0.0 (Foundation)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Appearance Preferences
            WalletQCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceXSmall))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = userPreferences.themeMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onThemeModeChanged(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Color",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Match system Material You wallpaper colors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userPreferences.dynamicColor,
                            onCheckedChange = onDynamicColorChanged
                        )
                    }
                }
            }

            // Currency Preferences
            WalletQCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Paid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Default Reporting Currency",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.spaceXSmall))
                Text(
                    text = "Amounts will be aggregated and displayed in this currency by default.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    Currency.SUPPORTED_CURRENCIES.forEach { currency ->
                        val isSelected = userPreferences.defaultCurrencyCode.equals(currency.code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onDefaultCurrencyChanged(currency.code) },
                            label = { Text("${currency.symbol} ${currency.code}") },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Privacy Guarantees Card
            WalletQCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Privacy Guarantees",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                PrivacyRow(title = "No Internet Permission", desc = "WalletQ does not request internet access.")
                PrivacyRow(title = "No Advertisements or Tracking", desc = "Zero telemetry, analytics, or monetization SDKs.")
                PrivacyRow(title = "Local Device Storage", desc = "Your financial database stays strictly on your device.")
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
        }
    }
}

@Composable
private fun PrivacyRow(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = Dimensions.spaceSmall)
        )
        Column {
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
