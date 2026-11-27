package com.walletq.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQMonetaryTypography
import com.walletq.core.designsystem.WalletQThemeTokens

@Composable
fun WalletQAccountRowCard(
    name: String,
    subtitle: String,
    formattedBalance: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    isNegativeBalance: Boolean = false,
    isArchived: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val tokens = WalletQThemeTokens.colors
    val resolvedAccent = accentColor ?: MaterialTheme.colorScheme.primary

    WalletQCard(
        modifier = modifier.alpha(if (isArchived) 0.65f else 1f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.iconBadgeContainer)
                    .clip(CircleShape)
                    .background(resolvedAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedAccent,
                    modifier = Modifier.size(Dimensions.iconMedium)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedBalance,
                    style = WalletQMonetaryTypography.medium,
                    color = if (isNegativeBalance) tokens.error else tokens.primaryText
                )
                if (isArchived) {
                    WalletQStatusBadge(
                        text = "Archived",
                        tone = WalletQStatusTone.ERROR
                    )
                }
            }
        }
    }
}

@Composable
fun WalletQTransactionRow(
    title: String,
    subtitle: String,
    formattedAmount: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    amountTone: WalletQStatusTone = WalletQStatusTone.NEUTRAL,
    isReversed: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val tokens = WalletQThemeTokens.colors
    val amountColor = when {
        isReversed -> tokens.secondaryText
        amountTone == WalletQStatusTone.SUCCESS -> tokens.success
        amountTone == WalletQStatusTone.ERROR -> tokens.error
        amountTone == WalletQStatusTone.WARNING -> tokens.warning
        else -> tokens.primaryText
    }

    WalletQCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimensions.iconMedium)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.primaryText,
                    textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formattedAmount,
                style = WalletQMonetaryTypography.medium,
                color = amountColor,
                textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}
