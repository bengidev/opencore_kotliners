package io.github.bengidev.opencore.chat.presenter

import android.content.Context
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View.MeasureSpec
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView

/**
 * TextView-backed growing text for live assistant output. Appends deltas instead of
 * rebuilding Compose [androidx.compose.material3.Text] layout on every flush.
 */
@Composable
internal fun ChatStreamingTextView(
    text: String,
    textStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    isTextSelectable: Boolean = true,
    showsCursor: Boolean = false,
    cursorColor: Color = color,
    cursorOpacity: Float = 1f,
) {
    val coordinator = remember { StreamingTextCoordinator() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ChatStreamingSizingTextView(context).apply {
                isEnabled = true
                isFocusable = false
                isClickable = false
                isLongClickable = isTextSelectable
                setHorizontallyScrolling(false)
                maxLines = Int.MAX_VALUE
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { textView ->
            textView.setTextIsSelectable(isTextSelectable)
            coordinator.apply(
                text = text,
                textView = textView,
                textStyle = textStyle,
                color = color,
                showsCursor = showsCursor,
                cursorColor = cursorColor,
                cursorOpacity = cursorOpacity,
            )
        },
        onRelease = { textView ->
            coordinator.cancel(textView)
        },
    )
}

private class StreamingTextCoordinator {
    private var boundTextView: ChatStreamingSizingTextView? = null
    private var appliedText = ""
    private var appliedShowsCursor = false
    private var appliedCursorOpacity = 1f
    private var pendingText = ""
    private var pendingShowsCursor = false
    private var pendingCursorColor: Color = Color.Unspecified
    private var pendingCursorOpacity = 1f
    private var updateRunnable: Runnable? = null
    private var lastLayoutInvalidationUptimeMs = 0L

    fun apply(
        text: String,
        textView: ChatStreamingSizingTextView,
        textStyle: TextStyle,
        color: Color,
        showsCursor: Boolean,
        cursorColor: Color,
        cursorOpacity: Float,
    ) {
        if (boundTextView !== textView) {
            boundTextView?.let(::cancel)
            boundTextView = textView
            resetAppliedState()
        }
        pendingText = text
        pendingShowsCursor = showsCursor
        pendingCursorColor = cursorColor
        pendingCursorOpacity = cursorOpacity
        if (updateRunnable != null) return

        val runnable = Runnable {
            updateRunnable = null
            flushPending(textView, textStyle, color)
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
        appliedShowsCursor = false
        appliedCursorOpacity = 1f
        lastLayoutInvalidationUptimeMs = 0L
    }

    private fun flushPending(
        textView: ChatStreamingSizingTextView,
        textStyle: TextStyle,
        color: Color,
    ) {
        if (!textView.isAttachedToWindow) return

        val text = pendingText
        val showsCursor = pendingShowsCursor
        val cursorOpacity = pendingCursorOpacity
        val cursorColorArgb = pendingCursorColor.withCursorOpacity(cursorOpacity).toArgb()

        if (ChatStreamingTextCursorPolicy.shouldUpdateCursorAttributesOnly(
                appliedText = appliedText,
                newText = text,
                appliedShowsCursor = appliedShowsCursor,
                showsCursor = showsCursor,
                appliedCursorOpacity = appliedCursorOpacity,
                newCursorOpacity = cursorOpacity,
            ) && !textView.text.isNullOrEmpty()
        ) {
            textView.updateInlineCursorColor(cursorColorArgb)
            appliedCursorOpacity = cursorOpacity
            return
        }

        if (!ChatStreamingTextCursorPolicy.shouldRebuild(
                appliedText = appliedText,
                newText = text,
                appliedShowsCursor = appliedShowsCursor,
                showsCursor = showsCursor,
                appliedCursorOpacity = appliedCursorOpacity,
                newCursorOpacity = cursorOpacity,
            )
        ) {
            return
        }

        textView.applyStreamingStyle(textStyle, color)
        val textColorArgb = color.toArgb()

        when (val update = ChatStreamingTextAppendPolicy.decide(appliedText, text)) {
            ChatStreamingTextUpdate.Unchanged -> {
                textView.setStreamingContent(text, textColorArgb, showsCursor, cursorColorArgb)
            }
            is ChatStreamingTextUpdate.AppendDelta -> {
                if (appliedShowsCursor) {
                    textView.removeInlineCursor()
                }
                textView.appendStyledDelta(update.delta, textColorArgb)
                if (showsCursor) {
                    textView.appendInlineCursor(cursorColorArgb)
                }
            }
            is ChatStreamingTextUpdate.ReplaceAll -> {
                textView.setStreamingContent(update.text, textColorArgb, showsCursor, cursorColorArgb)
            }
        }

        appliedText = text
        appliedShowsCursor = showsCursor
        appliedCursorOpacity = cursorOpacity
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

/** Non-scrolling [TextView] that caches height for Compose layout. */
internal class ChatStreamingSizingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextView(context, attrs) {
    private var measuredWidth = 0
    private var cachedMeasuredHeight: Int? = null
    private var measuredLength = 0
    private var layoutInvalidationScheduled = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val length = text?.length ?: 0
        val cachedHeight = cachedMeasuredHeight
        if (cachedHeight != null && width == measuredWidth && length == measuredLength) {
            setMeasuredDimension(width, cachedHeight)
            return
        }

        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        measuredWidth = width
        cachedMeasuredHeight = measuredHeight
        measuredLength = length
    }

    fun invalidateMeasuredHeight() {
        cachedMeasuredHeight = null
        if (!isAttachedToWindow || layoutInvalidationScheduled) return
        layoutInvalidationScheduled = true
        postOnAnimation {
            layoutInvalidationScheduled = false
            if (isAttachedToWindow) {
                requestLayout()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != measuredWidth) {
            invalidateMeasuredHeight()
        }
    }
}

private fun Color.withCursorOpacity(opacity: Float): Color =
    copy(alpha = alpha * opacity.coerceIn(0f, 1f))

private fun TextView.setStreamingContent(
    content: String,
    textColorArgb: Int,
    showsCursor: Boolean,
    cursorColorArgb: Int,
) {
    val builder = SpannableStringBuilder(content)
    if (content.isNotEmpty()) {
        builder.setSpan(
            ForegroundColorSpan(textColorArgb),
            0,
            builder.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    if (showsCursor) {
        val start = builder.length
        builder.append(ChatStreamingTextCursorPolicy.GLYPH)
        builder.setSpan(
            ForegroundColorSpan(cursorColorArgb),
            start,
            builder.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    text = builder
}

private fun TextView.ensureEditable(): Editable {
    val current = text
    if (current is Editable) return current
    val builder = SpannableStringBuilder(current ?: "")
    setText(builder, TextView.BufferType.EDITABLE)
    return builder
}

private fun TextView.appendStyledDelta(delta: String, textColorArgb: Int) {
    if (delta.isEmpty()) return
    val editable = ensureEditable()
    val start = editable.length
    editable.append(delta)
    editable.setSpan(
        ForegroundColorSpan(textColorArgb),
        start,
        editable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

private fun TextView.appendInlineCursor(cursorColorArgb: Int) {
    val editable = ensureEditable()
    val start = editable.length
    editable.append(ChatStreamingTextCursorPolicy.GLYPH)
    editable.setSpan(
        ForegroundColorSpan(cursorColorArgb),
        start,
        editable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

private fun TextView.removeInlineCursor() {
    val editable = text as? Editable ?: return
    if (editable.isEmpty()) return
    val lastIndex = editable.length - 1
    if (editable[lastIndex].toString() != ChatStreamingTextCursorPolicy.GLYPH) return
    editable.delete(lastIndex, editable.length)
}

private fun TextView.updateInlineCursorColor(cursorColorArgb: Int) {
    val editable = text as? Editable ?: return
    if (editable.isEmpty()) return
    val start = editable.length - 1
    editable.getSpans(start, editable.length, ForegroundColorSpan::class.java).forEach(editable::removeSpan)
    editable.setSpan(
        ForegroundColorSpan(cursorColorArgb),
        start,
        editable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

private fun TextView.applyStreamingStyle(style: TextStyle, color: Color) {
    setTextColor(color.toArgb())
    setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
    typeface = style.fontFamily.resolveTypeface(style.fontWeight ?: FontWeight.Normal)
    val lineHeight = style.lineHeight
    if (lineHeight != null && lineHeight.isSpecified) {
        val fontSizePx = style.fontSize.value * resources.displayMetrics.scaledDensity
        val lineHeightPx = lineHeight.value * resources.displayMetrics.scaledDensity
        val extra = (lineHeightPx - fontSizePx).coerceAtLeast(0f)
        setLineSpacing(extra, 1f)
    }
}

private fun FontFamily?.resolveTypeface(weight: FontWeight): Typeface {
    val family = when (this) {
        FontFamily.Monospace -> Typeface.MONOSPACE
        FontFamily.SansSerif, null -> Typeface.SANS_SERIF
        else -> Typeface.SANS_SERIF
    }
    val style = when (weight) {
        FontWeight.Bold,
        FontWeight.SemiBold,
        FontWeight.Medium,
        FontWeight.Black,
        FontWeight.ExtraBold,
        -> Typeface.BOLD
        else -> Typeface.NORMAL
    }
    return Typeface.create(family, style)
}
