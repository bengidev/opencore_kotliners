package io.github.bengidev.opencore.onboarding.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Color tokens for OpenCore onboarding — typealias to [OpenCorePalette].
 */
internal typealias OnboardingPalette = OpenCorePalette

internal val LightOnboardingPalette: OnboardingPalette = LightOpenCorePalette
internal val DarkOnboardingPalette: OnboardingPalette = DarkOpenCorePalette

internal val LocalOnboardingPalette = staticCompositionLocalOf<OnboardingPalette> { LightOnboardingPalette }
