package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamingCoalescerTest {

    @Test
    fun accumulate_appendsTextAndThinkingDeltas() {
        val coalescer = ChatStreamingCoalescer()
        assertTrue(coalescer.accumulate(ChatStreamingEvent.ThinkingDelta("A")))
        assertTrue(coalescer.accumulate(ChatStreamingEvent.TextDelta("B")))
        assertEquals("A", coalescer.accumulatedThinking)
        assertEquals("B", coalescer.accumulatedText)
    }

    @Test
    fun accumulate_returnsFalseForTerminalEvents() {
        val coalescer = ChatStreamingCoalescer()
        assertFalse(coalescer.accumulate(ChatStreamingEvent.Done))
    }

    @Test
    fun reset_clearsBuffers() {
        val coalescer = ChatStreamingCoalescer()
        coalescer.accumulate(ChatStreamingEvent.TextDelta("Hi"))
        coalescer.reset()
        assertEquals("", coalescer.accumulatedText)
        assertEquals("", coalescer.accumulatedThinking)
        assertEquals(0, coalescer.pendingByteCount)
    }

    @Test
    fun pendingByteCount_usesLargerBuffer() {
        val coalescer = ChatStreamingCoalescer()
        coalescer.accumulate(ChatStreamingEvent.ThinkingDelta("12345"))
        coalescer.accumulate(ChatStreamingEvent.TextDelta("ab"))
        assertEquals(5, coalescer.pendingByteCount)
    }
}
