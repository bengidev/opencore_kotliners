package io.github.bengidev.opencore.chat.presenter

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatAssistantMarkdownPreprocessor
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer
import io.github.bengidev.opencore.chat.utilities.ChatRichContentSegment
import io.github.bengidev.opencore.chat.utilities.ChatRichContentSegmenter

@Composable
internal fun ChatRichContentColumn(
    markdown: String,
    profile: ChatMarkwonRenderer.Profile,
    modifier: Modifier = Modifier,
    isTextSelectable: Boolean = true,
) {
    val palette = ChatTheme.corePalette
    val context = LocalContext.current
    val segments = remember(markdown, profile) {
        ChatRichContentSegmenter.segment(
            ChatAssistantMarkdownPreprocessor.normalize(markdown),
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEach { segment ->
            when (segment) {
                is ChatRichContentSegment.Prose -> {
                    if (segment.markdown.isBlank()) return@forEach
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            TextView(ctx).apply {
                                movementMethod = LinkMovementMethod.getInstance()
                                setTextIsSelectable(isTextSelectable)
                            }
                        },
                        update = { tv ->
                            tv.text = ChatMarkwonRenderer.spanned(
                                markdown = segment.markdown,
                                palette = palette,
                                profile = profile,
                                context = context,
                            )
                        },
                    )
                }
                is ChatRichContentSegment.MermaidDiagram,
                is ChatRichContentSegment.MathBlock -> {
                    MarkdownEmbedWebView(
                        segment = segment,
                        palette = palette,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
