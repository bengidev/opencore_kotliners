package io.github.bengidev.opencore.chat.presenter

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.ui.text.TextStyle

internal fun TextView.configureMarkwonTextView(bodyStyle: TextStyle, textColorArgb: Int) {
    setHorizontallyScrolling(false)
    maxLines = Int.MAX_VALUE
    includeFontPadding = false
    setPadding(0, 0, 0, 0)
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    setTextColor(textColorArgb)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyStyle.fontSize.value)
}
