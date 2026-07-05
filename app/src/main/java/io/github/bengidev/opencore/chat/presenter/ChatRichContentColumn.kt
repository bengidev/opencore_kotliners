package io.github.bengidev.opencore.chat.presenter

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatAssistantMarkdownPreprocessor
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer
import io.github.bengidev.opencore.chat.utilities.ChatPlainTextRenderer
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
    showsStreamingCursor: Boolean = false,
    streamingCursorColor: Color = Color.Unspecified,
    streamingCursorOpacity: Float = 1f,
    streamingRawTextStyle: TextStyle? = null,
    streamingRawColor: Color? = null,
) {
    val palette = ChatTheme.corePalette
    val context = LocalContext.current
    val normalized = remember(markdown) { ChatAssistantMarkdownPreprocessor.normalize(markdown) }
    val segments = remember(normalized, profile, progressive) {
        ChatRichContentSegmenter.segment(normalized, progressive = progressive)
    }
    val lastSegmentIndex = segments.lastIndex
    val lastRawFragmentIndex = segments.indexOfLast { it is ChatRichContentSegment.RawFragment }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            val isStreamingTail = progressive && index == lastSegmentIndex
            key(segmentStableKey(segment, index)) {
                RichContentSegment(
                    segment = segment,
                    profile = profile,
                    palette = palette,
                    context = context,
                    isTextSelectable = isTextSelectable,
                    isStreamingTail = isStreamingTail,
                    showsStreamingCursor = showsStreamingCursor && index == lastRawFragmentIndex,
                    streamingCursorColor = streamingCursorColor,
                    streamingCursorOpacity = streamingCursorOpacity,
                    streamingRawTextStyle = streamingRawTextStyle,
                    streamingRawColor = streamingRawColor,
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
    showsStreamingCursor: Boolean,
    streamingCursorColor: Color,
    streamingCursorOpacity: Float,
    streamingRawTextStyle: TextStyle?,
    streamingRawColor: Color?,
) {
    when (segment) {
        is ChatRichContentSegment.Prose -> {
            if (segment.markdown.isBlank()) return
            if (isStreamingTail) {
                renderStreamingTail(
                    text = segment.markdown,
                    profile = profile,
                    palette = palette,
                    isTextSelectable = isTextSelectable,
                    showsCursor = showsStreamingCursor,
                    streamingCursorColor = streamingCursorColor,
                    streamingCursorOpacity = streamingCursorOpacity,
                    streamingRawTextStyle = streamingRawTextStyle,
                    streamingRawColor = streamingRawColor,
                )
            } else {
                FrozenMarkwonText(
                    markdown = segment.markdown,
                    profile = profile,
                    palette = palette,
                    context = context,
                    isTextSelectable = isTextSelectable,
                )
            }
        }
        is ChatRichContentSegment.RawFragment -> {
            if (segment.text.isBlank()) return
            renderStreamingTail(
                text = segment.text,
                profile = profile,
                palette = palette,
                isTextSelectable = isTextSelectable,
                showsCursor = showsStreamingCursor,
                streamingCursorColor = streamingCursorColor,
                streamingCursorOpacity = streamingCursorOpacity,
                streamingRawTextStyle = streamingRawTextStyle,
                streamingRawColor = streamingRawColor,
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

@Composable
private fun FrozenMarkwonText(
    markdown: String,
    profile: ChatMarkwonRenderer.Profile,
    palette: OpenCorePalette,
    context: Context,
    isTextSelectable: Boolean,
) {
    val bodyStyle = when (profile) {
        ChatMarkwonRenderer.Profile.Assistant -> ChatTheme.typography.assistantMessageBody
        ChatMarkwonRenderer.Profile.Thinking -> ChatTheme.typography.reasoningBody
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = TableAwareMovementMethod.create()
                setTextIsSelectable(isTextSelectable)
                configureMarkwonTextView(bodyStyle)
            }
        },
        update = { tv ->
            tv.setTextIsSelectable(isTextSelectable)
            tv.configureMarkwonTextView(bodyStyle)
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

private fun TextView.configureMarkwonTextView(bodyStyle: TextStyle) {
    setHorizontallyScrolling(false)
    maxLines = Int.MAX_VALUE
    includeFontPadding = false
    setPadding(0, 0, 0, 0)
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyStyle.fontSize.value)
}

@Composable
private fun renderStreamingTail(
    text: String,
    profile: ChatMarkwonRenderer.Profile,
    palette: OpenCorePalette,
    isTextSelectable: Boolean,
    showsCursor: Boolean,
    streamingCursorColor: Color,
    streamingCursorOpacity: Float,
    streamingRawTextStyle: TextStyle?,
    streamingRawColor: Color?,
) {
    val rawStyle = streamingRawTextStyle
    val rawColor = streamingRawColor
    if (rawStyle != null && rawColor != null) {
        ChatStreamingTextView(
            text = text,
            textStyle = rawStyle,
            color = rawColor,
            modifier = Modifier.fillMaxWidth(),
            isTextSelectable = isTextSelectable,
            showsCursor = showsCursor,
            cursorColor = streamingCursorColor,
            cursorOpacity = streamingCursorOpacity,
        )
    } else {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                TextView(ctx).apply {
                    setTextIsSelectable(isTextSelectable)
                }
            },
            update = { tv ->
                tv.text = when (profile) {
                    ChatMarkwonRenderer.Profile.Thinking ->
                        ChatPlainTextRenderer.spannedThinking(text, palette)
                    ChatMarkwonRenderer.Profile.Assistant ->
                        ChatPlainTextRenderer.spanned(text, palette)
                }
            },
        )
    }
}

/** Index-only keys keep AndroidViews alive while tail content grows during streaming. */
private fun segmentStableKey(segment: ChatRichContentSegment, index: Int): String =
    when (segment) {
        is ChatRichContentSegment.Prose -> "prose-$index"
        is ChatRichContentSegment.RawFragment -> "raw-$index"
        is ChatRichContentSegment.MermaidDiagram -> "mermaid-$index"
        is ChatRichContentSegment.MathBlock -> "math-$index"
    }
