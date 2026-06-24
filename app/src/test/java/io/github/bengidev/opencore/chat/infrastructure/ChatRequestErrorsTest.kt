package io.github.bengidev.opencore.chat.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatRequestErrorsTest {

    @Test
    fun formatChatRequestError_unknownHost_includesHostAndDnsHint() {
        val message = formatChatRequestError(
            UnknownHostException("""Unable to resolve host "openrouter.ai": No address associated with hostname""")
        )

        assertTrue(message.contains("openrouter.ai"))
        assertTrue(message.contains("DNS lookup failed"))
    }

    @Test
    fun formatChatRequestError_wrappedUnknownHost_unwrapsCause() {
        val message = formatChatRequestError(
            IOException(
                "failed to connect",
                UnknownHostException("""Unable to resolve host "openrouter.ai"""")
            )
        )

        assertTrue(message.contains("openrouter.ai"))
        assertTrue(message.contains("DNS lookup failed"))
    }

    @Test
    fun formatChatRequestError_socketTimeout_returnsTimeoutMessage() {
        val message = formatChatRequestError(SocketTimeoutException("timed out"))

        assertEquals("Request timed out. Check your connection and try again.", message)
    }

    @Test
    fun formatChatRequestError_wrappedSocketTimeout_unwrapsCause() {
        val message = formatChatRequestError(
            IOException("read failed", SocketTimeoutException("timed out"))
        )

        assertEquals("Request timed out. Check your connection and try again.", message)
    }
}
