package com.walletq.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.WalletQMotion
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.rememberReducedMotion

enum class WalletQStatusTone {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    NEUTRAL
}

@Composable
fun WalletQStatusBadge(
    text: String,
    tone: WalletQStatusTone = WalletQStatusTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val tokens = WalletQThemeTokens.colors
    val (containerColor, contentColor) = when (tone) {
        WalletQStatusTone.SUCCESS -> tokens.successContainer to tokens.success
        WalletQStatusTone.ERROR -> tokens.errorContainer to tokens.error
        WalletQStatusTone.WARNING -> tokens.warningContainer to tokens.warning
        WalletQStatusTone.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        WalletQStatusTone.NEUTRAL -> tokens.elevatedSurface to tokens.secondaryText
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = Dimensions.spaceSmall,
                vertical = Dimensions.spaceXSmall
            )
        )
    }
}

@Composable
fun WalletQProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    tone: WalletQStatusTone = WalletQStatusTone.INFO
) {
    val tokens = WalletQThemeTokens.colors
    val reducedMotion = rememberReducedMotion()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = WalletQMotion.standardSpec(reducedMotion),
        label = "progress_indicator_anim"
    )
    val indicatorColor = when (tone) {
        WalletQStatusTone.SUCCESS -> tokens.success
        WalletQStatusTone.ERROR -> tokens.error
        WalletQStatusTone.WARNING -> tokens.warning
        WalletQStatusTone.INFO -> MaterialTheme.colorScheme.primary
        WalletQStatusTone.NEUTRAL -> tokens.secondaryText
    }

    LinearProgressIndicator(
        progress = { animatedProgress },
        color = indicatorColor,
        trackColor = tokens.elevatedSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
    )
}
