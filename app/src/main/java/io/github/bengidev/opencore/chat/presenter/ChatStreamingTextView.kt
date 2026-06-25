package io.github.bengidev.opencore.chat.presenter

import android.content.Context
import android.graphics.Typeface
import android.os.SystemClock
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
            )
        },
        onRelease = { textView ->
            coordinator.cancel(textView)
        },
    )
}

private class StreamingTextCoordinator {
    private var appliedText = ""
    private var pendingText = ""
    private var updateRunnable: Runnable? = null
    private var lastLayoutInvalidationUptimeMs = 0L

    fun apply(
        text: String,
        textView: ChatStreamingSizingTextView,
        textStyle: TextStyle,
        color: Color,
    ) {
        pendingText = text
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
    }

    private fun flushPending(
        textView: ChatStreamingSizingTextView,
        textStyle: TextStyle,
        color: Color,
    ) {
        val text = pendingText
        when (val update = ChatStreamingTextAppendPolicy.decide(appliedText, text)) {
            ChatStreamingTextUpdate.Unchanged -> return
            is ChatStreamingTextUpdate.AppendDelta -> {
                textView.applyStreamingStyle(textStyle, color)
                textView.append(update.delta)
            }
            is ChatStreamingTextUpdate.ReplaceAll -> {
                textView.applyStreamingStyle(textStyle, color)
                textView.text = update.text
            }
        }

        appliedText = text
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
        requestLayout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != measuredWidth) {
            invalidateMeasuredHeight()
        }
    }
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
