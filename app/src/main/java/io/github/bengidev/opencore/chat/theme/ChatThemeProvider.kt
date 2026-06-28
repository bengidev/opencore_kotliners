package io.github.bengidev.opencore.chat.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.bengidev.opencore.home.theme.HomeTheme

@Composable
internal fun OpenCoreChatTheme(content: @Composable () -> Unit) {
    val palette = HomeTheme.palette
    CompositionLocalProvider(
        LocalChatPalette provides ChatPaletteDefaults.fromPalette(palette),
        LocalCorePalette provides palette,
        LocalChatTypography provides ChatTypographyDefaults.default
    ) {
        content()
    }
}
