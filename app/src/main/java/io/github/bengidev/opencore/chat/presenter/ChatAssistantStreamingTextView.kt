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
import io.github.bengidev.opencore.chat.utilities.ChatAssistantMarkdownRenderer
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette

/**
 * Markdown-aware streaming assistant text — full attributed rebuild per flush.
 * Mirrors iOS `ChatAssistantStreamingTextView`.
 */
@Composable
internal fun ChatAssistantStreamingTextView(
    text: String,
    modifier: Modifier = Modifier,
    isTextSelectable: Boolean = true,
) {
    val palette = HomeTheme.palette
    val typography = ChatTheme.typography
    val coordinator = remember { AssistantMarkdownStreamingCoordinator() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ChatStreamingSizingTextView(context).apply {
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, typography.assistantMessageBody.fontSize.value)
            }
        },
        update = { textView ->
            textView.setTextIsSelectable(isTextSelectable)
            coordinator.apply(text = text, palette = palette, textView = textView)
        },
        onRelease = { textView ->
            coordinator.cancel(textView)
        },
    )
}

private class AssistantMarkdownStreamingCoordinator {
    private var boundTextView: ChatStreamingSizingTextView? = null
    private var appliedText = ""
    private var appliedIsDark: Boolean? = null
    private var pendingText = ""
    private var pendingPalette: OpenCorePalette? = null
    private var updateRunnable: Runnable? = null
    private var lastLayoutInvalidationUptimeMs = 0L

    fun apply(
        text: String,
        palette: OpenCorePalette,
        textView: ChatStreamingSizingTextView,
    ) {
        if (boundTextView !== textView) {
            boundTextView?.let(::cancel)
            boundTextView = textView
            resetAppliedState()
        }
        pendingText = text
        pendingPalette = palette
        if (updateRunnable != null) return

        val runnable = Runnable {
            updateRunnable = null
            flushPending(textView)
        }
        updateRunnable = runnable
        textView.postDelayed(runnable, COALESCE_DELAY_MS)
    }

    fun cancel(textView: ChatStreamingSizingTextView) {
        updateRunnable?.let(textView::removeCallbacks)
        updateRunnable = null
        if (boundTextView === textView) {
            boundTextView = null
            resetAppliedState()
        }
    }

    private fun resetAppliedState() {
        appliedText = ""
        appliedIsDark = null
        lastLayoutInvalidationUptimeMs = 0L
    }

    private fun flushPending(textView: ChatStreamingSizingTextView) {
        if (!textView.isAttachedToWindow || boundTextView !== textView) return

        val palette = pendingPalette ?: return
        val text = pendingText
        val isDark = palette.isDark
        if (text == appliedText && isDark == appliedIsDark) return

        textView.text = ChatAssistantMarkdownRenderer.spanned(text, palette)
        appliedText = text
        appliedIsDark = isDark

        val byteCount = appliedText.encodeToByteArray().size
        if (ChatStreamingTextAppendPolicy.shouldInvalidateLayout(lastLayoutInvalidationUptimeMs, byteCount)) {
            lastLayoutInvalidationUptimeMs = SystemClock.uptimeMillis()
            textView.invalidateMeasuredHeight()
        }
    }

    private companion object {
        const val COALESCE_DELAY_MS = 33L
    }
}
