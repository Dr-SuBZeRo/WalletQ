package com.walletq.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walletq.core.designsystem.Dimensions
import com.walletq.core.designsystem.VibrantElectricBlue
import com.walletq.core.designsystem.VibrantPurple
import com.walletq.core.designsystem.WalletQMotion
import com.walletq.core.designsystem.WalletQThemeTokens
import com.walletq.core.designsystem.rememberReducedMotion

/**
 * Vibrant 5-destination bottom navigation bar with an integrated central Add action.
 * Respects system navigation/gesture insets and reduced-motion preferences.
 */
@Composable
fun WalletQBottomBar(
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = WalletQThemeTokens.colors
    val reducedMotion = rememberReducedMotion()
    val destinations = TopLevelDestination.entries

    Surface(
        color = tokens.navBarBackground.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(
                thickness = Dimensions.cardBorderWidth,
                color = tokens.subtleBorder
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.navBarHeight)
                    .padding(horizontal = Dimensions.spaceXSmall),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left two destinations: Home, Activity
                destinations.take(2).forEach { destination ->
                    BottomNavItem(
                        destination = destination,
                        isSelected = currentRoute == destination.route,
                        reducedMotion = reducedMotion,
                        onClick = { onNavigate(destination) }
                    )
                }

                // Prominent central Add action
                val addInteraction = remember { MutableInteractionSource() }
                val addPressed by addInteraction.collectIsPressedAsState()
                val addScale by animateFloatAsState(
                    targetValue = if (addPressed && !reducedMotion) 0.92f else 1f,
                    animationSpec = WalletQMotion.fastSpec(reducedMotion),
                    label = "center_add_scale"
                )
                Box(
                    modifier = Modifier
                        .size(Dimensions.minTouchTarget)
                        .graphicsLayer {
                            scaleX = addScale
                            scaleY = addScale
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VibrantPurple, VibrantElectricBlue)
                            )
                        )
                        .clickable(
                            interactionSource = addInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = onAddClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Quick Add Transaction",
                        tint = Color.White,
                        modifier = Modifier.size(Dimensions.iconLarge)
                    )
                }

                // Right three destinations: Accounts, Reports, More
                destinations.drop(2).forEach { destination ->
                    BottomNavItem(
                        destination = destination,
                        isSelected = currentRoute == destination.route,
                        reducedMotion = reducedMotion,
                        onClick = { onNavigate(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    destination: TopLevelDestination,
    isSelected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val tokens = WalletQThemeTokens.colors
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            tokens.secondaryText
        },
        animationSpec = WalletQMotion.fastSpec(reducedMotion),
        label = "nav_content_color_${destination.route}"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) tokens.navIndicator else Color.Transparent,
        animationSpec = WalletQMotion.fastSpec(reducedMotion),
        label = "nav_indicator_color_${destination.route}"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && !reducedMotion) 1.06f else 1f,
        animationSpec = WalletQMotion.fastSpec(reducedMotion),
        label = "nav_icon_scale_${destination.route}"
    )

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = Dimensions.minTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(indicatorColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = destination.title,
                tint = contentColor,
                modifier = Modifier
                    .size(Dimensions.iconMedium)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = destination.title,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
