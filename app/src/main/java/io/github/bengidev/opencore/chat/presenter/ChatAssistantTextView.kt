package io.github.bengidev.opencore.chat.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer

/**
 * Assistant answer text with deferred rich rendering.
 * Progressive plain tail while streaming; full markdown/LaTeX/Mermaid when complete.
 */
@Composable
internal fun ChatAssistantTextView(
    text: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    isTextSelectable: Boolean = true,
) {
    val palette = ChatTheme.corePalette
    val typography = ChatTheme.typography

    if (isStreaming) {
        key("assistant-streaming") {
            ChatRichContentColumn(
                markdown = text,
                profile = ChatMarkwonRenderer.Profile.Assistant,
                modifier = modifier,
                isTextSelectable = isTextSelectable,
                progressive = true,
                streamingRawTextStyle = typography.assistantMessageBody,
                streamingRawColor = palette.textPrimary,
            )
        }
    } else {
        key("assistant-rich") {
            ChatRichContentColumn(
                markdown = text,
                profile = ChatMarkwonRenderer.Profile.Assistant,
                modifier = modifier,
                isTextSelectable = isTextSelectable,
            )
        }
    }
}
