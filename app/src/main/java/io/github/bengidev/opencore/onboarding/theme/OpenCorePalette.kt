package io.github.bengidev.opencore.onboarding.theme

import androidx.compose.ui.graphics.Color

/**
 * Graphite monochrome palette for OpenCore onboarding UI.
 * OpenCorepalette tokens (OpenCore branding).
 */

// ===== Raw iOS-faithful tokens (light) =====

private val LightSurfaceBase = Color(0xFFF7F7F7)
private val LightSurfacePaper = Color(0xFFF1F1F1)
private val LightSurfaceRaised = Color(0xFFFFFFFF)
private val LightSurfaceSubtle = Color(0xFFEAEAEA)
private val LightSurfaceGalaxyTint = Color(0xFFE2E2E2)
private val LightTextPrimary = Color(0xFF141414)
private val LightTextSecondary = Color(0xFF6E6E6E)
private val LightTextTertiary = Color(0xFF9C9C9C)
private val LightLineSoft = Color(0xFFE0E0E0)
private val LightLineStrong = Color(0xFFBEBEBE)
private val LightAccentPrimary = Color(0xFF2B2B2B)     // graphite
private val LightAccentDeep = Color(0xFF0F0F0F)
private val LightAccentSoft = Color(0xFFE2E2E2)
private val LightControlStrong = Color(0xFF141414)
private val LightControlStrongText = Color(0xFFFFFFFF)
private val LightSuccess = Color(0xFF4A4A4A)
private val LightWarning = Color(0xFF333333)
private val LightDanger = Color(0xFF1A1A1A)

// ===== Raw iOS-faithful tokens (dark) =====

private val DarkSurfaceBase = Color(0xFF0B0B0B)
private val DarkSurfacePaper = Color(0xFF121212)
private val DarkSurfaceRaised = Color(0xFF1A1A1A)
private val DarkSurfaceSubtle = Color(0xFF242424)
private val DarkSurfaceGalaxyTint = Color(0xFF2C2C2C)
private val DarkTextPrimary = Color(0xFFF5F5F5)
private val DarkTextSecondary = Color(0xFFB0B0B0)
private val DarkTextTertiary = Color(0xFF7E7E7E)
private val DarkLineSoft = Color(0xFF2E2E2E)
private val DarkLineStrong = Color(0xFF484848)
private val DarkAccentPrimary = Color(0xFFDADADA)
private val DarkAccentDeep = Color(0xFFF4F4F4)
private val DarkAccentSoft = Color(0xFF2C2C2C)
private val DarkControlStrong = Color(0xFFF5F5F5)
private val DarkControlStrongText = Color(0xFF121212)
private val DarkSuccess = Color(0xFFB5B5B5)
private val DarkWarning = Color(0xFFCECECE)
private val DarkDanger = Color(0xFFEDEDED)

/**
 * The single source of truth for color tokens on Android. Mirrors
 * iOS `OpenCorePalette` 1:1 and exposes Android-friendly aliases so
 * Home/Chat call-sites don't need to know about iOS naming.
 */
internal data class OpenCorePalette(
    val isDark: Boolean,

    // -- iOS-faithful (source of truth) --
    val surfaceBase: Color,
    val surfacePaper: Color,
    val surfaceRaised: Color,
    val surfaceSubtle: Color,
    val surfaceGalaxyTint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val lineSoft: Color,
    val lineStrong: Color,
    val accentPrimary: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val controlStrong: Color,
    val controlStrongText: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,

    // -- Android aliases (for existing call-sites) --
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val inverseSurface: Color,
    val border: Color,
    val strongBorder: Color,
    val accent: Color,
    val accentText: Color,
    val primaryActionFill: Color,
    val primaryActionText: Color,
    val orbTint: Color,
    val textMuted: Color
)

internal val LightOpenCorePalette = OpenCorePalette(
    isDark = false,

    surfaceBase = LightSurfaceBase,
    surfacePaper = LightSurfacePaper,
    surfaceRaised = LightSurfaceRaised,
    surfaceSubtle = LightSurfaceSubtle,
    surfaceGalaxyTint = LightSurfaceGalaxyTint,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    lineSoft = LightLineSoft,
    lineStrong = LightLineStrong,
    accentPrimary = LightAccentPrimary,
    accentDeep = LightAccentDeep,
    accentSoft = LightAccentSoft,
    controlStrong = LightControlStrong,
    controlStrongText = LightControlStrongText,
    success = LightSuccess,
    warning = LightWarning,
    danger = LightDanger,

    background = LightSurfaceBase,
    backgroundSecondary = LightSurfacePaper,
    surface = LightSurfaceRaised,
    elevatedSurface = LightSurfaceSubtle,
    inverseSurface = LightControlStrong,
    border = LightLineSoft,
    strongBorder = LightLineStrong,
    accent = LightAccentPrimary,
    accentText = LightControlStrongText,
    primaryActionFill = LightControlStrong,
    primaryActionText = LightControlStrongText,
    orbTint = LightTextTertiary,
    textMuted = LightTextTertiary
)

internal val DarkOpenCorePalette = OpenCorePalette(
    isDark = true,

    surfaceBase = DarkSurfaceBase,
    surfacePaper = DarkSurfacePaper,
    surfaceRaised = DarkSurfaceRaised,
    surfaceSubtle = DarkSurfaceSubtle,
    surfaceGalaxyTint = DarkSurfaceGalaxyTint,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    lineSoft = DarkLineSoft,
    lineStrong = DarkLineStrong,
    accentPrimary = DarkAccentPrimary,
    accentDeep = DarkAccentDeep,
    accentSoft = DarkAccentSoft,
    controlStrong = DarkControlStrong,
    controlStrongText = DarkControlStrongText,
    success = DarkSuccess,
    warning = DarkWarning,
    danger = DarkDanger,

    background = DarkSurfaceBase,
    backgroundSecondary = DarkSurfacePaper,
    surface = DarkSurfaceRaised,
    elevatedSurface = DarkSurfaceSubtle,
    inverseSurface = DarkControlStrong,
    border = DarkLineSoft,
    strongBorder = DarkLineStrong,
    accent = DarkAccentPrimary,
    accentText = DarkControlStrongText,
    primaryActionFill = DarkControlStrong,
    primaryActionText = DarkControlStrongText,
    orbTint = DarkTextTertiary,
    textMuted = DarkTextTertiary
)
