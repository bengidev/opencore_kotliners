package io.github.bengidev.opencore.home.theme

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.bengidev.opencore.onboarding.theme.DarkOpenCorePalette
import io.github.bengidev.opencore.onboarding.theme.LightOpenCorePalette
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette

internal typealias HomePalette = OpenCorePalette

internal val LightHomePalette: HomePalette = LightOpenCorePalette
internal val DarkHomePalette: HomePalette = DarkOpenCorePalette

internal val LocalHomePalette = staticCompositionLocalOf<HomePalette> { LightHomePalette }
