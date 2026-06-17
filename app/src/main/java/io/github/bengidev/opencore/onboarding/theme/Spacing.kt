package io.github.bengidev.opencore.onboarding.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for OpenCore onboarding.
 * 4dp rhythm for compact, 8dp for standard spacing.
 */
@Immutable
internal data class OnboardingSpacing(
    val space1: Dp,
    val space2: Dp,
    val space3: Dp,
    val space4: Dp,
    val space5: Dp,
    val space6: Dp,
    val space7: Dp,
    val space8: Dp
)

internal val DefaultOnboardingSpacing = OnboardingSpacing(
    space1 = 6.dp,
    space2 = 10.dp,
    space3 = 14.dp,
    space4 = 18.dp,
    space5 = 22.dp,
    space6 = 28.dp,
    space7 = 36.dp,
    space8 = 44.dp
)

internal val LocalOnboardingSpacing = staticCompositionLocalOf { DefaultOnboardingSpacing }
