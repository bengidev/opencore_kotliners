package io.github.bengidev.opencore.chat.presenter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReasoningCollapsePolicyTest {
    @Test
    fun shouldAutoCollapse_whenThinkingStreamingAndCompetingStream_returnsTrue() {
        assertTrue(
            ChatReasoningCollapsePolicy.shouldAutoCollapse(
                hasCompetingStream = true,
                isThinkingStreaming = true,
            ),
        )
    }

    @Test
    fun shouldAutoCollapse_whenNoCompetingStream_returnsFalse() {
        assertFalse(
            ChatReasoningCollapsePolicy.shouldAutoCollapse(
                hasCompetingStream = false,
                isThinkingStreaming = true,
            ),
        )
    }

    @Test
    fun shouldAutoCollapse_whenNotThinkingStreaming_returnsFalse() {
        assertFalse(
            ChatReasoningCollapsePolicy.shouldAutoCollapse(
                hasCompetingStream = true,
                isThinkingStreaming = false,
            ),
        )
    }
}
