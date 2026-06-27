package io.github.bengidev.opencore.home.presenter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

internal object HomeContextUsagePopoverMotion {
    fun presentationAnimationSpec(reduceMotion: Boolean): FiniteAnimationSpec<Float> =
        if (reduceMotion) {
            tween(durationMillis = 160, easing = FastOutSlowInEasing)
        } else {
            spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMediumLow,
            )
        }

    @Composable
    fun rememberReduceMotion(): Boolean {
        val context = LocalContext.current
        return Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** Tap-outside scrim for the context usage popover. Covers scroll areas only. */
@Composable
internal fun HomeContextUsageDismissScrim(
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (reduceMotion) 0.001f else 0.06f))
            .clickable(onClick = onDismiss),
    )
}

@Composable
internal fun HomeContextUsagePopoverAnimated(
    visible: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spec = HomeContextUsagePopoverMotion.presentationAnimationSpec(reduceMotion)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) {
            fadeIn(spec)
        } else {
            fadeIn(spec) + scaleIn(
                animationSpec = spec,
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(1f, 1f),
            )
        },
        exit = if (reduceMotion) {
            fadeOut(spec)
        } else {
            fadeOut(spec) + scaleOut(
                animationSpec = spec,
                targetScale = 0.97f,
                transformOrigin = TransformOrigin(1f, 1f),
            )
        },
    ) {
        content()
    }
}