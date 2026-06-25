package io.github.bengidev.opencore.chat.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamingTextAppendPolicyTest {
    private val strategy = PrefixAppendChatStreamingTextAppendStrategy

    @Test
    fun decide_returnsUnchangedWhenTextMatches() {
        assertEquals(
            ChatStreamingTextUpdate.Unchanged,
            strategy.decide("Hello", "Hello"),
        )
    }

    @Test
    fun decide_appendsDeltaWhenNewTextExtendsAppliedText() {
        assertEquals(
            ChatStreamingTextUpdate.AppendDelta(" world"),
            strategy.decide("Hello", "Hello world"),
        )
    }

    @Test
    fun decide_replacesWhenNewTextDoesNotExtendAppliedText() {
        assertEquals(
            ChatStreamingTextUpdate.ReplaceAll("Hi"),
            strategy.decide("Hello", "Hi"),
        )
    }

    @Test
    fun decide_replacesWhenNewTextIsShorter() {
        assertEquals(
            ChatStreamingTextUpdate.ReplaceAll("Hel"),
            strategy.decide("Hello", "Hel"),
        )
    }

    @Test
    fun decide_replacesWhenSameLengthButDifferentContent() {
        assertEquals(
            ChatStreamingTextUpdate.ReplaceAll("Jello"),
            strategy.decide("Hello", "Jello"),
        )
    }

    @Test
    fun layoutInvalidationIntervalMs_usesDefaultForSmallOutput() {
        assertEquals(50L, ChatStreamingTextAppendPolicy.layoutInvalidationIntervalMs(0))
        assertEquals(50L, ChatStreamingTextAppendPolicy.layoutInvalidationIntervalMs(7_999))
    }

    @Test
    fun layoutInvalidationIntervalMs_scalesToMediumAndLargeOutput() {
        assertEquals(150L, ChatStreamingTextAppendPolicy.layoutInvalidationIntervalMs(8_000))
        assertEquals(150L, ChatStreamingTextAppendPolicy.layoutInvalidationIntervalMs(31_999))
        assertEquals(250L, ChatStreamingTextAppendPolicy.layoutInvalidationIntervalMs(32_000))
    }

    @Test
    fun shouldInvalidateLayout_respectsThrottleWindow() {
        assertTrue(ChatStreamingTextAppendPolicy.shouldInvalidateLayout(0L, 0, nowUptimeMs = 50L))
        assertFalse(ChatStreamingTextAppendPolicy.shouldInvalidateLayout(0L, 0, nowUptimeMs = 49L))
        assertTrue(ChatStreamingTextAppendPolicy.shouldInvalidateLayout(0L, 32_000, nowUptimeMs = 250L))
        assertFalse(ChatStreamingTextAppendPolicy.shouldInvalidateLayout(0L, 32_000, nowUptimeMs = 249L))
    }
}
