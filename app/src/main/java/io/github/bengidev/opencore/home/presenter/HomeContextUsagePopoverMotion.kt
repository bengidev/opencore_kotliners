package io.github.bengidev.opencore.home.presenter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

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
}
