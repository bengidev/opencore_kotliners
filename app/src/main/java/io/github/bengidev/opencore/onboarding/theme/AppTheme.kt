package io.github.bengidev.opencore.onboarding.theme

import androidx.compose.runtime.compositionLocalOf

/** Theme preference — mirrors OpenCoreAppTheme (system → light → dark → system). */
internal enum class AppTheme {
    System,
    Light,
    Dark;

    internal fun resolveDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    val next: AppTheme
        get() = when (this) {
            System -> Light
            Light -> Dark
            Dark -> System
        }
}

internal val LocalAppTheme = compositionLocalOf { AppTheme.System }
