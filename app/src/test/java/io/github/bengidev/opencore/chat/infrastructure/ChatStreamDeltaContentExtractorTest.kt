package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.utilities.ChatStreamContentPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatStreamDeltaContentExtractorTest {

    @Test
    fun extractContentText_stringDelta() {
        val payload = """{"choices":[{"delta":{"content":"Hello"}}]}"""
        assertEquals("Hello", ChatStreamDeltaContentExtractor.extractContentText(payload))
    }

    @Test
    fun extractContentText_arrayDelta() {
        val payload = """{"choices":[{"delta":{"content":[{"type":"text","text":"Hi"}]}}]}"""
        assertEquals("Hi", ChatStreamDeltaContentExtractor.extractContentText(payload))
    }

    @Test
    fun extractContentText_missingContentReturnsNull() {
        assertNull(ChatStreamDeltaContentExtractor.extractContentText("""{"choices":[{"delta":{}}]}"""))
    }

    @Test
    fun extractContentParts_commandOutputBegin() {
        val payload = """{"choices":[{"delta":{"content":[{"type":"output_stream_begin","command":"npm test","cwd":"/tmp"}]}}]}"""
        val parts = ChatStreamDeltaContentExtractor.extractContentParts(payload)
        assertEquals(1, parts.size)
        assertEquals("npm test", parts.first().resolvedCommand)
        assertEquals("/tmp", parts.first().cwd)
    }

    @Test
    fun extractContentParts_commandArrayJoinsWithSpaces() {
        val payload = """{"choices":[{"delta":{"content":[{"type":"output_stream_begin","command":["npm","test"]}]}}]}"""
        val parts = ChatStreamDeltaContentExtractor.extractContentParts(payload)
        assertEquals("npm test", parts.first().resolvedCommand)
    }
}
