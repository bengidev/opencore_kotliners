package io.github.bengidev.opencore.sidepanel.theme

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.bengidev.opencore.onboarding.theme.DarkOpenCorePalette
import io.github.bengidev.opencore.onboarding.theme.LightOpenCorePalette
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette

internal typealias SidePanelPalette = OpenCorePalette

internal val LightSidePanelPalette: SidePanelPalette = LightOpenCorePalette
internal val DarkSidePanelPalette: SidePanelPalette = DarkOpenCorePalette

internal val LocalSidePanelPalette = staticCompositionLocalOf<SidePanelPalette> { LightSidePanelPalette }
