package io.github.bengidev.opencore.chat.presenter

/** Pure cursor blink + update decisions for inline streaming caret (Policy pattern). */
internal object ChatStreamingTextCursorPolicy {
    const val GLYPH = "▍"
    const val BLINK_HALF_PERIOD_MS = 550L
    private const val MAX_OPACITY = 1f
    private const val MIN_OPACITY = 0.2f

    fun opacityAt(elapsedMs: Long): Float {
        val cycleMs = BLINK_HALF_PERIOD_MS * 2
        val phase = (elapsedMs % cycleMs) / BLINK_HALF_PERIOD_MS.toFloat()
        val t = if (phase <= 1f) phase else 2f - phase
        return MAX_OPACITY + (MIN_OPACITY - MAX_OPACITY) * t
    }

    fun shouldUpdateCursorAttributesOnly(
        appliedText: String,
        newText: String,
        appliedShowsCursor: Boolean,
        showsCursor: Boolean,
        appliedCursorOpacity: Float,
        newCursorOpacity: Float,
    ): Boolean = appliedText == newText &&
        showsCursor &&
        appliedShowsCursor &&
        newCursorOpacity != appliedCursorOpacity

    fun shouldRebuild(
        appliedText: String,
        newText: String,
        appliedShowsCursor: Boolean,
        showsCursor: Boolean,
        appliedCursorOpacity: Float,
        newCursorOpacity: Float,
    ): Boolean = appliedText != newText ||
        showsCursor != appliedShowsCursor ||
        (
            !shouldUpdateCursorAttributesOnly(
                appliedText,
                newText,
                appliedShowsCursor,
                showsCursor,
                appliedCursorOpacity,
                newCursorOpacity,
            ) && newCursorOpacity != appliedCursorOpacity
            )
}
