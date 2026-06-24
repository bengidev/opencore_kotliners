package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatStreamingCodecTest {

    @Test
    fun mapDataPayload_textDelta() {
        val events = ChatStreamingCodec.mapDataPayload(
            """{"choices":[{"delta":{"content":"hello"}}]}"""
        )

        assertEquals(listOf(ChatStreamingEvent.TextDelta("hello")), events)
    }

    @Test
    fun mapDataPayload_reasoningDelta() {
        val events = ChatStreamingCodec.mapDataPayload(
            """{"choices":[{"delta":{"reasoning":"think"}}]}"""
        )

        assertEquals(listOf(ChatStreamingEvent.ThinkingDelta("think")), events)
    }

    @Test
    fun mapDataPayload_reasoningContentField() {
        val events = ChatStreamingCodec.mapDataPayload(
            """{"choices":[{"delta":{"reasoning_content":"note"}}]}"""
        )

        assertEquals(listOf(ChatStreamingEvent.ThinkingDelta("note")), events)
    }

    @Test
    fun mapDataPayload_errorEnvelope() {
        val events = ChatStreamingCodec.mapDataPayload(
            """{"error":{"message":"Invalid API key"}}"""
        )

        assertEquals(
            listOf(ChatStreamingEvent.Error(ChatStreamError("Invalid API key"))),
            events
        )
    }

    @Test
    fun mapDataPayload_skipsWhitespaceOnlyReasoning() {
        val events = ChatStreamingCodec.mapDataPayload(
            """{"choices":[{"delta":{"reasoning":"   ","content":"ok"}}]}"""
        )

        assertEquals(listOf(ChatStreamingEvent.TextDelta("ok")), events)
    }

    @Test
    fun mapDataPayload_emptyDeltaReturnsNull() {
        assertNull(ChatStreamingCodec.mapDataPayload("""{"choices":[{"delta":{}}]}"""))
    }
}
