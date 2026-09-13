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
import androidx.compose.ui.text.font.FontStyle
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
                clipToOutline = true
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
            )
        },
        onRelease = { textView ->
            coordinator.cancel(textView)
        },
    )
}

private class StreamingTextCoordinator {
    private val scheduler = CoalescedTextViewScheduler()
    private var boundTextView: ChatStreamingSizingTextView? = null
    private var appliedText = ""
    private var pendingText = ""
    private var pendingTextStyle: TextStyle? = null
    private var pendingColor: Color? = null
    private var lastLayoutInvalidationUptimeMs = 0L

    fun apply(
        text: String,
        textView: ChatStreamingSizingTextView,
        textStyle: TextStyle,
        color: Color,
    ) {
        boundTextView = textView
        pendingText = text
        pendingTextStyle = textStyle
        pendingColor = color
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
        appliedText = ""
        lastLayoutInvalidationUptimeMs = 0L
    }

    private fun flushPending(textView: ChatStreamingSizingTextView) {
        val textStyle = pendingTextStyle ?: return
        val color = pendingColor ?: return
        if (!textView.isAttachedToWindow || boundTextView !== textView) return

        val text = pendingText
        if (text == appliedText) return

        textView.applyStreamingStyle(textStyle, color)
        val textColorArgb = color.toArgb()

        when (val update = ChatStreamingTextAppendPolicy.decide(appliedText, text)) {
            ChatStreamingTextUpdate.Unchanged -> Unit
            is ChatStreamingTextUpdate.AppendDelta -> {
                textView.appendStyledDelta(
                    delta = update.delta,
                    textColorArgb = textColorArgb,
                )
            }
            is ChatStreamingTextUpdate.ReplaceAll -> {
                textView.setStreamingContent(update.text, textColorArgb)
            }
        }

        appliedText = text
        val byteCount = appliedText.encodeToByteArray().size
        if (ChatStreamingTextAppendPolicy.shouldInvalidateLayout(lastLayoutInvalidationUptimeMs, byteCount)) {
            lastLayoutInvalidationUptimeMs = SystemClock.uptimeMillis()
            textView.invalidateMeasuredHeight()
        }
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

private fun TextView.setStreamingContent(
    content: String,
    textColorArgb: Int,
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
    text = builder
}

private fun TextView.ensureEditable(): Editable? {
    if (!isAttachedToWindow) return null
    val current = text
    if (current is Editable) return current
    val builder = SpannableStringBuilder(current ?: "")
    setText(builder, TextView.BufferType.EDITABLE)
    return text as? Editable
}

private fun TextView.appendStyledDelta(
    delta: String,
    textColorArgb: Int,
) {
    if (delta.isEmpty()) return
    val editable = ensureEditable() ?: return
    val start = editable.length
    editable.append(delta)
    editable.setSpan(
        ForegroundColorSpan(textColorArgb),
        start,
        editable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

private fun TextView.applyStreamingStyle(style: TextStyle, color: Color) {
    setTextColor(color.toArgb())
    setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
    typeface = style.resolveAndroidTypeface()
    val lineHeight = style.lineHeight
    if (lineHeight != null && lineHeight.isSpecified) {
        val fontSizePx = style.fontSize.value * resources.displayMetrics.scaledDensity
        val lineHeightPx = lineHeight.value * resources.displayMetrics.scaledDensity
        val extra = (lineHeightPx - fontSizePx).coerceAtLeast(0f)
        setLineSpacing(extra, 1f)
    }
}

private fun TextStyle.resolveAndroidTypeface(): Typeface {
    val family = when (fontFamily) {
        FontFamily.Monospace -> Typeface.MONOSPACE
        FontFamily.SansSerif, null -> Typeface.SANS_SERIF
        else -> Typeface.SANS_SERIF
    }
    val weight = fontWeight ?: FontWeight.Normal
    val bold = weight >= FontWeight.Medium
    val italic = fontStyle == FontStyle.Italic
    val styleBits = (if (bold) Typeface.BOLD else 0) or (if (italic) Typeface.ITALIC else 0)
    return Typeface.create(family, styleBits)
}
