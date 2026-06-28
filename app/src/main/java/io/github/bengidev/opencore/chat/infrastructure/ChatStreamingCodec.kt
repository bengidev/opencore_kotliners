package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent

internal object ChatStreamingCodec {
    fun mapDataPayload(payload: String): List<ChatStreamingEvent>? {
        ProviderStreamOutputEventMapper.mapSidebandPayload(payload)?.let { return it }

        ChatCompletionsCodec.parseErrorMessage(payload)?.let { message ->
            return listOf(ChatStreamingEvent.Error(ChatStreamError(message)))
        }

        val events = mutableListOf<ChatStreamingEvent>()
        val reasoning = ChatJsonStringField.extract(payload, "reasoning")
            ?: ChatJsonStringField.extract(payload, "reasoning_content")
        if (!reasoning.isNullOrBlank() && reasoning.trim().isNotEmpty()) {
            events += ChatStreamingEvent.ThinkingDelta(reasoning)
        }
        val contentParts = ChatStreamDeltaContentExtractor.extractContentParts(payload)
        if (contentParts.isNotEmpty()) {
            events += ProviderStreamOutputEventMapper.mapContentParts(contentParts)
            contentParts.mapNotNull { it.renderedText }.joinToString("")
                .takeIf { it.isNotEmpty() }
                ?.let { events += ChatStreamingEvent.TextDelta(it) }
        } else {
            ChatStreamDeltaContentExtractor.extractLegacyContentText(payload)
                ?.takeIf { it.isNotEmpty() }
                ?.let { events += ChatStreamingEvent.TextDelta(it) }
        }
        return events.takeIf { it.isNotEmpty() }
    }
}
