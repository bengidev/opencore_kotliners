package io.github.bengidev.opencore.onboarding.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * OpenCore onboarding theme.
 * Provides design tokens (palette, typography, spacing, radius) via CompositionLocals.
 */
@Composable
internal fun OpenCoreOnboardingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = if (darkTheme) AppTheme.Dark else AppTheme.Light,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkOnboardingPalette else LightOnboardingPalette

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalOnboardingPalette provides palette,
        LocalOnboardingTypography provides DefaultOnboardingTypography,
        LocalOnboardingSpacing provides DefaultOnboardingSpacing,
        LocalOnboardingRadius provides DefaultOnboardingRadius,
        content = content
    )
}

/**
 * Convenience accessors for design tokens.
 */
internal object OnboardingTheme {
    val palette: OnboardingPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalOnboardingPalette.current

    val typography: OnboardingTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOnboardingTypography.current

    val spacing: OnboardingSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalOnboardingSpacing.current

    val radius: OnboardingRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalOnboardingRadius.current
}
