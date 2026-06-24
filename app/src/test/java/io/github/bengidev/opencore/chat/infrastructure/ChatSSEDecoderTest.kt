package io.github.bengidev.opencore.chat.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSSEDecoderTest {

    @Test
    fun interpret_dataLineAndDoneSentinel() {
        assertEquals(
            ChatSSEDecoder.SseEvent.Data("hello"),
            ChatSSEDecoder.interpret("data: hello")
        )
        assertEquals(ChatSSEDecoder.SseEvent.Done, ChatSSEDecoder.interpret("data: [DONE]"))
        assertNull(ChatSSEDecoder.interpret(": keep-alive"))
        assertNull(ChatSSEDecoder.interpret(""))
    }

    @Test
    fun interpret_stripsCrlfLineEnding() {
        assertEquals(
            ChatSSEDecoder.SseEvent.Data("ok"),
            ChatSSEDecoder.interpret("data: ok\r")
        )
    }

    @Test
    fun append_buffersPartialLinesAcrossChunks() {
        val decoder = ChatSSEDecoder()
        assertTrue(decoder.append("data: hel".toByteArray()).isEmpty())
        assertEquals(
            listOf(ChatSSEDecoder.SseEvent.Data("hello")),
            decoder.append("lo\n".toByteArray())
        )
    }

    @Test
    fun flush_emitsTrailingLineWithoutNewline() {
        val decoder = ChatSSEDecoder()
        assertTrue(decoder.append("data: tail".toByteArray()).isEmpty())
        assertEquals(
            listOf(ChatSSEDecoder.SseEvent.Data("tail")),
            decoder.flush()
        )
    }
}
