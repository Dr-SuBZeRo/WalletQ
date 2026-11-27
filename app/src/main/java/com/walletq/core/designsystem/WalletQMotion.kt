package com.walletq.core.designsystem

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Centralized motion specifications for WalletQ.
 *
 * Principles:
 * - Fast, crisp, non-bouncy transitions (150ms–260ms).
 * - Respects Android's ANIMATOR_DURATION_SCALE reduced-motion accessibility setting.
 * - Never interpolates fake intermediate monetary amounts.
 */
object WalletQMotion {
    const val FAST_DURATION_MS = 150
    const val STANDARD_DURATION_MS = 220
    const val SHEET_DURATION_MS = 260

    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fastSpec(reducedMotion: Boolean = false): FiniteAnimationSpec<T> = tween(
        durationMillis = if (reducedMotion) 0 else FAST_DURATION_MS,
        easing = StandardEasing
    )

    fun <T> standardSpec(reducedMotion: Boolean = false): FiniteAnimationSpec<T> = tween(
        durationMillis = if (reducedMotion) 0 else STANDARD_DURATION_MS,
        easing = StandardEasing
    )

    fun screenEnter(reducedMotion: Boolean = false): EnterTransition =
        fadeIn(animationSpec = fastSpec(reducedMotion))

    fun screenExit(reducedMotion: Boolean = false): ExitTransition =
        fadeOut(animationSpec = fastSpec(reducedMotion))

    fun contentEnter(reducedMotion: Boolean = false): EnterTransition =
        if (reducedMotion) {
            EnterTransition.None
        } else {
            fadeIn(animationSpec = standardSpec(false)) +
                slideInVertically(animationSpec = standardSpec(false)) { it / 12 }
        }

    fun contentExit(reducedMotion: Boolean = false): ExitTransition =
        if (reducedMotion) {
            ExitTransition.None
        } else {
            fadeOut(animationSpec = fastSpec(false))
        }

    /**
     * Discrete crossfade for formatted monetary values or status labels.
     * Displays the exact final value immediately after a brief crossfade without
     * rolling through misleading intermediate numbers.
     */
    fun <S> discreteValueTransition(reducedMotion: Boolean = false): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        if (reducedMotion) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            (fadeIn(animationSpec = fastSpec(false)) +
                slideInVertically(animationSpec = fastSpec(false)) { it / 6 })
                .togetherWith(
                    fadeOut(animationSpec = fastSpec(false)) +
                        slideOutVertically(animationSpec = fastSpec(false)) { -it / 6 }
                )
        }
    }
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}
