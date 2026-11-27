package com.walletq.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQMonetaryTypography
import com.walletq.core.designsystem.WalletQMotion
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.rememberReducedMotion

@Composable
fun WalletQSummaryCard(
    label: String,
    formattedAmount: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    amountColor: Color? = null,
    badge: (@Composable () -> Unit)? = null,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val tokens = WalletQThemeTokens.colors
    val reducedMotion = rememberReducedMotion()

    WalletQCard(
        modifier = modifier,
        elevated = elevated,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tokens.secondaryText
            )
            if (badge != null) {
                badge()
            }
        }
        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        AnimatedContent(
            targetState = formattedAmount,
            transitionSpec = WalletQMotion.discreteValueTransition(reducedMotion),
            label = "summary_amount_transition"
        ) { amount ->
            Text(
                text = amount,
                style = WalletQMonetaryTypography.hero,
                color = amountColor ?: tokens.primaryText
            )
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(Dimensions.spaceXSmall))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.secondaryText
            )
        }
    }
}

@Composable
fun WalletQSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val tokens = WalletQThemeTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = tokens.primaryText
        )
        when {
            action != null -> action()
            trailingText != null -> Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.secondaryText
            )
        }
    }
}
