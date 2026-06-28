package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent

/** Accumulates stream deltas between UI flushes (State pattern). */
internal class ChatStreamingCoalescer {
    var accumulatedText: String = ""
        private set
    var accumulatedThinking: String = ""
        private set
    var accumulatedOutputStreamDelta: String = ""
        private set

    val pendingByteCount: Int
        get() = maxOf(
            accumulatedText.encodeToByteArray().size,
            accumulatedThinking.encodeToByteArray().size,
            accumulatedOutputStreamDelta.encodeToByteArray().size,
        )

    fun reset() {
        accumulatedText = ""
        accumulatedThinking = ""
        accumulatedOutputStreamDelta = ""
    }

    fun consumeOutputStreamDelta(): String {
        val delta = accumulatedOutputStreamDelta
        accumulatedOutputStreamDelta = ""
        return delta
    }

    fun accumulate(event: ChatStreamingEvent): Boolean = when (event) {
        is ChatStreamingEvent.ThinkingDelta -> {
            accumulatedThinking += event.text
            true
        }
        is ChatStreamingEvent.TextDelta -> {
            accumulatedText += event.text
            true
        }
        is ChatStreamingEvent.OutputStreamDelta -> {
            accumulatedOutputStreamDelta += event.delta
            true
        }
        else -> false
    }
}
