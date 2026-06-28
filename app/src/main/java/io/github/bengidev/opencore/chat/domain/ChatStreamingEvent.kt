package io.github.bengidev.opencore.chat.domain

internal sealed class ChatStreamingEvent {
    data class TextDelta(val text: String) : ChatStreamingEvent()
    data class ThinkingDelta(val text: String) : ChatStreamingEvent()
    data class OutputStreamBegan(val command: String, val cwd: String?) : ChatStreamingEvent()
    data class OutputStreamDelta(val delta: String) : ChatStreamingEvent()
    data class OutputStreamEnded(
        val status: ChatOutputStreamStatus,
        val exitCode: Int?,
        val durationMs: Int?,
    ) : ChatStreamingEvent()
    data object Done : ChatStreamingEvent()
    data class Error(val error: ChatStreamError) : ChatStreamingEvent()
}
