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
    fun mapDataPayload_arrayContentDelta() {
        val events = ChatStreamingCodec.mapDataPayload(
            """{"choices":[{"delta":{"content":[{"type":"text","text":"Hi"}]}}]}"""
        )

        assertEquals(listOf(ChatStreamingEvent.TextDelta("Hi")), events)
    }

    @Test
    fun mapDataPayload_emptyDeltaReturnsNull() {
        assertNull(ChatStreamingCodec.mapDataPayload("""{"choices":[{"delta":{}}]}"""))
    }

    @Test
    fun mapDataPayload_sidebandExecCommandEvents() {
        val began = ChatStreamingCodec.mapDataPayload(
            """{"type":"exec_command_begin","command":"npm test","cwd":"/tmp/project"}"""
        )
        assertEquals(
            listOf(ChatStreamingEvent.OutputStreamBegan(command = "npm test", cwd = "/tmp/project")),
            began,
        )

        val delta = ChatStreamingCodec.mapDataPayload(
            """{"type":"exec_command_output_delta","chunk":"PASS suite\n"}"""
        )
        assertEquals(listOf(ChatStreamingEvent.OutputStreamDelta("PASS suite\n")), delta)
    }

    @Test
    fun mapDataPayload_commandOutputContentParts() {
        val payload =
            """{"choices":[{"delta":{"content":[{"type":"exec_command_begin","command":"git status","cwd":"/repo"},{"type":"exec_command_output_delta","chunk":"clean\\n"}]}}]}"""
        val events = ChatStreamingCodec.mapDataPayload(payload)
        assertEquals(
            listOf(
                ChatStreamingEvent.OutputStreamBegan(command = "git status", cwd = "/repo"),
                ChatStreamingEvent.OutputStreamDelta("clean\\n"),
            ),
            events,
        )
    }
}
