package io.github.bengidev.opencore.chat.presenter

import android.content.Context
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatAssistantMarkdownPreprocessor
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer
import io.github.bengidev.opencore.chat.utilities.ChatRichContentSegment
import io.github.bengidev.opencore.chat.utilities.ChatRichContentSegmenter
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import io.noties.markwon.ext.tables.TableAwareMovementMethod

@Composable
internal fun ChatRichContentColumn(
    markdown: String,
    profile: ChatMarkwonRenderer.Profile,
    modifier: Modifier = Modifier,
    isTextSelectable: Boolean = true,
    progressive: Boolean = false,
    streamingRawTextStyle: TextStyle? = null,
) {
    val palette = ChatTheme.corePalette
    val context = LocalContext.current
    val normalized = remember(markdown) { ChatAssistantMarkdownPreprocessor.normalize(markdown) }
    val segments = remember(normalized, profile, progressive) {
        ChatRichContentSegmenter.segment(normalized, progressive = progressive)
    }
    val lastSegmentIndex = segments.lastIndex

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            val isStreamingTail = progressive && index == lastSegmentIndex
            key(segmentComposeKey(segment, index, isStreamingTail)) {
                RichContentSegment(
                    segment = segment,
                    profile = profile,
                    palette = palette,
                    context = context,
                    isTextSelectable = isTextSelectable,
                    isStreamingTail = isStreamingTail,
                    streamingRawTextStyle = streamingRawTextStyle,
                )
            }
        }
    }
}

@Composable
private fun RichContentSegment(
    segment: ChatRichContentSegment,
    profile: ChatMarkwonRenderer.Profile,
    palette: OpenCorePalette,
    context: Context,
    isTextSelectable: Boolean,
    isStreamingTail: Boolean,
    streamingRawTextStyle: TextStyle?,
) {
    val defaultBodyStyle = when (profile) {
        ChatMarkwonRenderer.Profile.Assistant -> ChatTheme.typography.assistantMessageBody
        ChatMarkwonRenderer.Profile.Thinking -> ChatTheme.typography.reasoningBody
    }
    val bodyStyle = streamingRawTextStyle ?: defaultBodyStyle
    val textColorArgb = when (profile) {
        ChatMarkwonRenderer.Profile.Assistant -> palette.textPrimary
        ChatMarkwonRenderer.Profile.Thinking -> palette.textSecondary
    }.toArgb()

    when (segment) {
        is ChatRichContentSegment.Prose -> {
            if (segment.markdown.isBlank()) return
            if (isStreamingTail) {
                ChatStreamingMarkwonTail(
                    markdown = segment.markdown,
                    profile = profile,
                    palette = palette,
                    context = context,
                    bodyStyle = bodyStyle,
                    textColorArgb = textColorArgb,
                    isTextSelectable = isTextSelectable,
                )
            } else {
                FrozenMarkwonText(
                    markdown = segment.markdown,
                    profile = profile,
                    palette = palette,
                    context = context,
                    bodyStyle = bodyStyle,
                    textColorArgb = textColorArgb,
                    isTextSelectable = isTextSelectable,
                )
            }
        }
        is ChatRichContentSegment.RawFragment -> {
            if (segment.text.isBlank()) return
            if (isStreamingTail) {
                ChatStreamingMarkwonTail(
                    markdown = segment.text,
                    profile = profile,
                    palette = palette,
                    context = context,
                    bodyStyle = bodyStyle,
                    textColorArgb = textColorArgb,
                    isTextSelectable = isTextSelectable,
                )
            } else {
                FrozenMarkwonText(
                    markdown = segment.text,
                    profile = profile,
                    palette = palette,
                    context = context,
                    bodyStyle = bodyStyle,
                    textColorArgb = textColorArgb,
                    isTextSelectable = isTextSelectable,
                )
            }
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

@Composable
private fun FrozenMarkwonText(
    markdown: String,
    profile: ChatMarkwonRenderer.Profile,
    palette: OpenCorePalette,
    context: Context,
    bodyStyle: TextStyle,
    textColorArgb: Int,
    isTextSelectable: Boolean,
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = TableAwareMovementMethod.create()
                setTextIsSelectable(isTextSelectable)
                configureMarkwonTextView(bodyStyle, textColorArgb)
            }
        },
        update = { tv ->
            tv.setTextIsSelectable(isTextSelectable)
            tv.configureMarkwonTextView(bodyStyle, textColorArgb)
            ChatMarkwonRenderer.applyTo(
                textView = tv,
                markdown = markdown,
                palette = palette,
                profile = profile,
                context = context,
            )
        },
    )
}

private fun TextView.configureMarkwonTextView(bodyStyle: TextStyle, textColorArgb: Int) {
    setHorizontallyScrolling(false)
    maxLines = Int.MAX_VALUE
    includeFontPadding = false
    setPadding(0, 0, 0, 0)
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    setTextColor(textColorArgb)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyStyle.fontSize.value)
}

/** Frozen segments key by content so index shifts do not recycle AndroidViews. */
private fun segmentComposeKey(
    segment: ChatRichContentSegment,
    index: Int,
    isStreamingTail: Boolean,
): String {
    if (isStreamingTail) return "tail-$index"
    return when (segment) {
        is ChatRichContentSegment.Prose -> "prose-${segment.markdown.hashCode()}"
        is ChatRichContentSegment.RawFragment -> "raw-${segment.text.hashCode()}"
        is ChatRichContentSegment.MermaidDiagram -> "mermaid-${segment.source.hashCode()}"
        is ChatRichContentSegment.MathBlock -> "math-${segment.latex.hashCode()}"
    }
}

