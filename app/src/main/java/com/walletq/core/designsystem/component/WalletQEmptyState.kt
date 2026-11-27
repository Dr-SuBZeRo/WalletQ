package com.walletq.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQThemeTokens

@Composable
fun WalletQEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    val tokens = WalletQThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.spaceLarge, vertical = Dimensions.spaceMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.iconHero)
                .clip(CircleShape)
                .background(tokens.elevatedSurface)
                .border(Dimensions.cardBorderWidth, tokens.subtleBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconLarge),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = tokens.primaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.secondaryText,
            textAlign = TextAlign.Center
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            actionButton()
        }
    }
}
