package io.github.bengidev.opencore.chat.presenter

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.TextView

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
