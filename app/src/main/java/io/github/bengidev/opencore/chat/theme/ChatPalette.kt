package io.github.bengidev.opencore.chat.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.bengidev.opencore.home.theme.HomePalette

/** Chat-specific palette aliases over `OpenCorePalette`. Mirrors iOS chat tokens. */
@Immutable
internal data class ChatPalette(
    val userBubble: Color,
    val userBubbleText: Color,
    val assistantBubble: Color,
    val assistantBubbleText: Color,
    val reasoningCard: Color,
    val reasoningBorder: Color,
    val reasoningText: Color,
    val streamingDot: Color,
    val systemMessageText: Color,
    val messageMetaText: Color,
    val errorIcon: Color
)

internal object ChatPaletteDefaults {
    fun fromPalette(palette: HomePalette): ChatPalette = ChatPalette(
        userBubble = palette.controlStrong,
        userBubbleText = palette.controlStrongText,
        assistantBubble = palette.surfaceRaised,
        assistantBubbleText = palette.textPrimary,
        reasoningCard = blend(palette.surfaceBase, palette.surfaceRaised, 0.55f),
        reasoningBorder = palette.textTertiary.copy(alpha = 0.12f),
        reasoningText = palette.textSecondary,
        streamingDot = palette.accentPrimary,
        systemMessageText = palette.textSecondary,
        messageMetaText = palette.textTertiary,
        errorIcon = palette.danger
    )
}

private fun blend(a: Color, b: Color, t: Float): Color = Color(
    red = a.red * (1 - t) + b.red * t,
    green = a.green * (1 - t) + b.green * t,
    blue = a.blue * (1 - t) + b.blue * t,
    alpha = a.alpha * (1 - t) + b.alpha * t
)

internal val LocalChatPalette = staticCompositionLocalOf {
    ChatPaletteDefaults.fromPalette(
        io.github.bengidev.opencore.home.theme.LightHomePalette
    )
}

internal object ChatTheme {
    val palette: ChatPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalChatPalette.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = LocalChatTypography.current
}
