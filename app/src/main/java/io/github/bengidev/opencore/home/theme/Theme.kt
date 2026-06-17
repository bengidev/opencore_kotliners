package io.github.bengidev.opencore.home.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
internal fun OpenCoreHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkHomePalette else LightHomePalette

    CompositionLocalProvider(
        LocalHomePalette provides palette,
        LocalHomeTypography provides DefaultHomeTypography,
        content = content
    )
}

internal object HomeTheme {
    val palette: HomePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalHomePalette.current

    val typography: HomeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalHomeTypography.current
}
