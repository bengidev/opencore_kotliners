package io.github.bengidev.opencore.chat.domain

internal sealed class ChatStreamingEvent {
    data class TextDelta(val text: String) : ChatStreamingEvent()
    data class ThinkingDelta(val text: String) : ChatStreamingEvent()
    data object Done : ChatStreamingEvent()
    data class Error(val error: ChatStreamError) : ChatStreamingEvent()
}
