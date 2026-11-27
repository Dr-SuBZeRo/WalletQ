package com.walletq.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQThemeTokens

@Composable
fun WalletQLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val tokens = WalletQThemeTokens.colors
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            trackColor = tokens.elevatedSurface,
            strokeWidth = 3.dp
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.secondaryText
            )
        }
    }
}

@Composable
fun WalletQLoadingPlaceholder(
    modifier: Modifier = Modifier,
    count: Int = 3
) {
    val tokens = WalletQThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(Dimensions.itemSpacing)
    ) {
        repeat(count) {
            WalletQCard {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(tokens.elevatedSurface)
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(20.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(tokens.elevatedSurface)
                )
            }
        }
    }
}
