package io.github.bengidev.opencore.chat.presenter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer

/**
 * Assistant answer text with progressive markdown rendering while streaming.
 * Full markdown/LaTeX/Mermaid when complete.
 */
@Composable
internal fun ChatAssistantTextView(
    text: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    isTextSelectable: Boolean = true,
) {
    val typography = ChatTheme.typography

    ChatRichContentColumn(
        markdown = text,
        profile = ChatMarkwonRenderer.Profile.Assistant,
        modifier = modifier,
        isTextSelectable = isTextSelectable,
        progressive = isStreaming,
        streamingRawTextStyle = typography.assistantMessageBody,
    )
}
