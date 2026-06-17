package io.github.bengidev.opencore.onboarding.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Radius tokens for OpenCore onboarding.
 * Small, intentional radius system.
 */
@Immutable
internal data class OnboardingRadius(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val pill: Dp
)

internal val DefaultOnboardingRadius = OnboardingRadius(
    xs = 6.dp,
    sm = 8.dp,
    md = 16.dp,
    lg = 20.dp,
    pill = 999.dp
)

internal val LocalOnboardingRadius = staticCompositionLocalOf { DefaultOnboardingRadius }
