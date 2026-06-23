package io.github.bengidev.opencore.chat.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Mirrors iOS `ChatTypography` / `OpenCoreTypography` chat tokens. */
@Immutable
internal data class ChatTypography(
    val userMessageBody: TextStyle,
    val assistantMessageBody: TextStyle,
    val reasoningBody: TextStyle,
    val reasoningHeader: TextStyle,
    val messageMeta: TextStyle,
    val streamingLabel: TextStyle,
    val systemMessage: TextStyle
)

private val Sans = FontFamily.SansSerif
private val Mono = FontFamily.Monospace

internal object ChatTypographyDefaults {
    val default = ChatTypography(
        userMessageBody = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        assistantMessageBody = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        reasoningBody = TextStyle(
            fontFamily = Mono,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp
        ),
        reasoningHeader = TextStyle(
            fontFamily = Mono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 15.sp
        ),
        messageMeta = TextStyle(
            fontFamily = Mono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 13.sp
        ),
        streamingLabel = TextStyle(
            fontFamily = Mono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 15.sp
        ),
        systemMessage = TextStyle(
            fontFamily = Sans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 16.sp
        )
    )
}

internal val LocalChatTypography = staticCompositionLocalOf { ChatTypographyDefaults.default }
