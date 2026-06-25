package io.github.bengidev.opencore.chat.presenter

import android.os.SystemClock

/** Pure append/replace decision for live streaming text (Strategy pattern). */
internal sealed interface ChatStreamingTextUpdate {
    data object Unchanged : ChatStreamingTextUpdate
    data class AppendDelta(val delta: String) : ChatStreamingTextUpdate
    data class ReplaceAll(val text: String) : ChatStreamingTextUpdate
}

internal fun interface ChatStreamingTextAppendStrategy {
    fun decide(appliedText: String, newText: String): ChatStreamingTextUpdate
}

/** Prefix-append when `newText` extends `appliedText`; otherwise full replace. */
internal object PrefixAppendChatStreamingTextAppendStrategy : ChatStreamingTextAppendStrategy {
    override fun decide(appliedText: String, newText: String): ChatStreamingTextUpdate {
        if (newText == appliedText) return ChatStreamingTextUpdate.Unchanged
        if (newText.length > appliedText.length && newText.startsWith(appliedText)) {
            return ChatStreamingTextUpdate.AppendDelta(newText.substring(appliedText.length))
        }
        return ChatStreamingTextUpdate.ReplaceAll(newText)
    }
}

internal object ChatStreamingTextAppendPolicy {
    private val defaultStrategy: ChatStreamingTextAppendStrategy = PrefixAppendChatStreamingTextAppendStrategy

    const val DEFAULT_LAYOUT_INTERVAL_MS = 50L
    const val MEDIUM_LAYOUT_INTERVAL_MS = 150L
    const val LARGE_LAYOUT_INTERVAL_MS = 250L
    const val MEDIUM_TEXT_BYTE_COUNT = 8_000
    const val LARGE_TEXT_BYTE_COUNT = 32_000

    fun decide(
        appliedText: String,
        newText: String,
        strategy: ChatStreamingTextAppendStrategy = defaultStrategy,
    ): ChatStreamingTextUpdate = strategy.decide(appliedText, newText)

    fun layoutInvalidationIntervalMs(byteCount: Int): Long = when {
        byteCount >= LARGE_TEXT_BYTE_COUNT -> LARGE_LAYOUT_INTERVAL_MS
        byteCount >= MEDIUM_TEXT_BYTE_COUNT -> MEDIUM_LAYOUT_INTERVAL_MS
        else -> DEFAULT_LAYOUT_INTERVAL_MS
    }

    fun shouldInvalidateLayout(
        lastInvalidationUptimeMs: Long,
        byteCount: Int,
        nowUptimeMs: Long = SystemClock.uptimeMillis(),
    ): Boolean = nowUptimeMs - lastInvalidationUptimeMs >= layoutInvalidationIntervalMs(byteCount)
}
