package io.github.bengidev.opencore.chat.application

/** Adaptive flush and scroll delays for batched streaming UI (Policy pattern). */
internal object ChatStreamingCoalescingPolicy {
    const val DEFAULT_FLUSH_DELAY_MS = 80L
    const val MEDIUM_FLUSH_DELAY_MS = 120L
    const val LARGE_FLUSH_DELAY_MS = 200L
    const val MEDIUM_TEXT_BYTE_COUNT = 8_000
    const val LARGE_TEXT_BYTE_COUNT = 32_000

    fun flushDelayMs(byteCount: Int): Long = when {
        byteCount >= LARGE_TEXT_BYTE_COUNT -> LARGE_FLUSH_DELAY_MS
        byteCount >= MEDIUM_TEXT_BYTE_COUNT -> MEDIUM_FLUSH_DELAY_MS
        else -> DEFAULT_FLUSH_DELAY_MS
    }

    fun scrollDelayMs(byteCount: Int): Long = when {
        byteCount >= LARGE_TEXT_BYTE_COUNT -> LARGE_FLUSH_DELAY_MS
        byteCount >= MEDIUM_TEXT_BYTE_COUNT -> MEDIUM_FLUSH_DELAY_MS
        else -> 0L
    }
}
