package io.github.bengidev.opencore.chat.application

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatStreamingCoalescingPolicyTest {

    @Test
    fun flushDelayMs_usesDefaultForSmallOutput() {
        assertEquals(80L, ChatStreamingCoalescingPolicy.flushDelayMs(0))
        assertEquals(80L, ChatStreamingCoalescingPolicy.flushDelayMs(7_999))
    }

    @Test
    fun flushDelayMs_scalesToMediumAndLargeOutput() {
        assertEquals(120L, ChatStreamingCoalescingPolicy.flushDelayMs(8_000))
        assertEquals(120L, ChatStreamingCoalescingPolicy.flushDelayMs(31_999))
        assertEquals(200L, ChatStreamingCoalescingPolicy.flushDelayMs(32_000))
    }

    @Test
    fun scrollDelayMs_isImmediateForSmallOutput() {
        assertEquals(0L, ChatStreamingCoalescingPolicy.scrollDelayMs(0))
        assertEquals(0L, ChatStreamingCoalescingPolicy.scrollDelayMs(7_999))
    }

    @Test
    fun scrollDelayMs_scalesToMediumAndLargeOutput() {
        assertEquals(120L, ChatStreamingCoalescingPolicy.scrollDelayMs(8_000))
        assertEquals(200L, ChatStreamingCoalescingPolicy.scrollDelayMs(32_000))
    }
}
