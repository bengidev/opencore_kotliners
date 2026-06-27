package io.github.bengidev.opencore.sidepanel.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTitlePolicyTest {

    @Test
    fun fromUserMessage_trimsAndCapsLength() {
        val long = "a".repeat(100)
        assertEquals(80, ConversationTitlePolicy.fromUserMessage(long).length)
        assertEquals("hello", ConversationTitlePolicy.fromUserMessage("  hello  "))
    }
}
