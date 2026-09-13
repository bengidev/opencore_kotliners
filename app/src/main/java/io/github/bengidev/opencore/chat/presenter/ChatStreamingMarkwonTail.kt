package io.github.bengidev.opencore.chat.presenter

import android.content.Context
import android.os.SystemClock
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bengidev.opencore.chat.utilities.ChatMarkwonRenderer
import io.github.bengidev.opencore.chat.utilities.ChatStreamingMarkdownSafeText
import io.github.bengidev.opencore.onboarding.theme.OpenCorePalette
import io.noties.markwon.ext.tables.TableAwareMovementMethod

@Composable
internal fun ChatStreamingMarkwonTail(
    markdown: String,
    profile: ChatMarkwonRenderer.Profile,
    palette: OpenCorePalette,
    context: Context,
    bodyStyle: TextStyle,
    textColorArgb: Int,
    modifier: Modifier = Modifier,
    isTextSelectable: Boolean = true,
) {
    val coordinator = remember { StreamingMarkwonCoordinator() }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            ChatStreamingSizingTextView(ctx).apply {
                movementMethod = TableAwareMovementMethod.create()
                setTextIsSelectable(isTextSelectable)
                configureMarkwonTextView(bodyStyle, textColorArgb)
            }
        },
        update = { textView ->
            textView.setTextIsSelectable(isTextSelectable)
            textView.configureMarkwonTextView(bodyStyle, textColorArgb)
            coordinator.apply(
                markdown = markdown,
                textView = textView,
                palette = palette,
                profile = profile,
                context = context,
            )
        },
        onRelease = { textView ->
            coordinator.cancel(textView)
        },
    )
}

private class StreamingMarkwonCoordinator {
    private val scheduler = CoalescedTextViewScheduler()
    private var boundTextView: ChatStreamingSizingTextView? = null
    private var pendingMarkdown = ""
    private var pendingPalette: OpenCorePalette? = null
    private var pendingProfile: ChatMarkwonRenderer.Profile? = null
    private var pendingContext: Context? = null
    private var appliedMarkdown = ""
    private var lastLayoutInvalidationUptimeMs = 0L

    fun apply(
        markdown: String,
        textView: ChatStreamingSizingTextView,
        palette: OpenCorePalette,
        profile: ChatMarkwonRenderer.Profile,
        context: Context,
    ) {
        boundTextView = textView
        pendingMarkdown = markdown
        pendingPalette = palette
        pendingProfile = profile
        pendingContext = context
        scheduler.schedule(
            textView = textView,
            onBindingChanged = ::resetAppliedState,
            onFlush = ::flushPending,
        )
    }

    fun cancel(textView: ChatStreamingSizingTextView) {
        scheduler.cancel(textView)
        if (boundTextView === textView) {
            boundTextView = null
            resetAppliedState()
        }
    }

    private fun resetAppliedState() {
        appliedMarkdown = ""
        lastLayoutInvalidationUptimeMs = 0L
    }

    private fun flushPending(textView: ChatStreamingSizingTextView) {
        val palette = pendingPalette ?: return
        val profile = pendingProfile ?: return
        val context = pendingContext ?: return
        if (!textView.isAttachedToWindow || boundTextView !== textView) return

        val markdown = pendingMarkdown
        val safeMarkdown = ChatStreamingMarkdownSafeText.sanitize(markdown)
        if (safeMarkdown == appliedMarkdown) return

        ChatMarkwonRenderer.applyTo(
            textView = textView,
            markdown = safeMarkdown,
            palette = palette,
            profile = profile,
            context = context,
        )
        appliedMarkdown = safeMarkdown

        val byteCount = safeMarkdown.encodeToByteArray().size
        if (ChatStreamingTextAppendPolicy.shouldInvalidateLayout(lastLayoutInvalidationUptimeMs, byteCount)) {
            lastLayoutInvalidationUptimeMs = SystemClock.uptimeMillis()
            textView.invalidateMeasuredHeight()
        }
    }
}

