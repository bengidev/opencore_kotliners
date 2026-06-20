package io.github.bengidev.opencore.sidepanel.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
internal fun OpenCoreSidePanelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkSidePanelPalette else LightSidePanelPalette

    CompositionLocalProvider(
        LocalSidePanelPalette provides palette,
        LocalSidePanelTypography provides DefaultSidePanelTypography,
        content = content
    )
}

internal object SidePanelTheme {
    val palette: SidePanelPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSidePanelPalette.current

    val typography: SidePanelTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSidePanelTypography.current
}
