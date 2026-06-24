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
}
