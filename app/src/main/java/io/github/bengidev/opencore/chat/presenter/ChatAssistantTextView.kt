package io.github.bengidev.opencore.chat.presenter

import android.os.SystemClock
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.theme.ChatTheme
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer
import io.github.bengidev.opencore.chat.utilities.ChatPlainTextRenderer
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette

/**
 * Assistant answer text with plain streaming and rich markdown when complete.
 * [isStreaming] coalesces full attributed rebuilds; completed messages render via Markwon.
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
        val coordinator = remember { AssistantPlainStreamingCoordinator() }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                ChatStreamingSizingTextView(context).apply {
                    configureAssistantPlainTextView(
                        isTextSelectable = isTextSelectable,
                        fontSizeSp = typography.assistantMessageBody.fontSize.value,
                    )
                }
            },
            update = { textView ->
                textView.setTextIsSelectable(isTextSelectable)
                coordinator.apply(text = text, palette = palette, textView = textView)
            },
            onRelease = coordinator::cancel,
        )
    } else {
        ChatRichContentColumn(
            markdown = text,
            profile = ChatMarkwonRenderer.Profile.Assistant,
            modifier = modifier,
            isTextSelectable = isTextSelectable,
        )
    }
}

private fun TextView.configureAssistantPlainTextView(
    isTextSelectable: Boolean,
    fontSizeSp: Float,
) {
    isEnabled = true
    isFocusable = false
    isClickable = false
    isLongClickable = isTextSelectable
    movementMethod = LinkMovementMethod.getInstance()
    clipToOutline = true
    setHorizontallyScrolling(false)
    maxLines = Int.MAX_VALUE
    includeFontPadding = false
    setPadding(0, 0, 0, 0)
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
}

private class AssistantPlainStreamingCoordinator {
    private val scheduler = CoalescedTextViewScheduler()
    private var appliedText = ""
    private var appliedIsDark: Boolean? = null
    private var pendingText = ""
    private var pendingPalette: OpenCorePalette? = null
    private var lastLayoutInvalidationUptimeMs = 0L

    fun apply(
        text: String,
        palette: OpenCorePalette,
        textView: ChatStreamingSizingTextView,
    ) {
        pendingText = text
        pendingPalette = palette
        scheduler.schedule(
            textView = textView,
            onBindingChanged = ::resetAppliedState,
            onFlush = ::flushPending,
        )
    }

    fun cancel(textView: ChatStreamingSizingTextView) {
        scheduler.cancel(textView)
        resetAppliedState()
    }

    private fun resetAppliedState() {
        appliedText = ""
        appliedIsDark = null
        lastLayoutInvalidationUptimeMs = 0L
    }

    private fun flushPending(textView: ChatStreamingSizingTextView) {
        val palette = pendingPalette ?: return
        val text = pendingText
        val isDark = palette.isDark
        if (text == appliedText && isDark == appliedIsDark) return

        textView.text = ChatPlainTextRenderer.spanned(text, palette)
        appliedText = text
        appliedIsDark = isDark

        val byteCount = appliedText.encodeToByteArray().size
        if (ChatStreamingTextAppendPolicy.shouldInvalidateLayout(lastLayoutInvalidationUptimeMs, byteCount)) {
            lastLayoutInvalidationUptimeMs = SystemClock.uptimeMillis()
            textView.invalidateMeasuredHeight()
        }
    }
}
