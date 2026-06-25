package io.github.bengidev.opencore.chat.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamingTextCursorPolicyTest {
    @Test
    fun opacityAt_startsAtMaxOpacity() {
        assertEquals(1f, ChatStreamingTextCursorPolicy.opacityAt(0L), 0.001f)
    }

    @Test
    fun opacityAt_reachesMinOpacityAtHalfPeriod() {
        assertEquals(
            0.2f,
            ChatStreamingTextCursorPolicy.opacityAt(ChatStreamingTextCursorPolicy.BLINK_HALF_PERIOD_MS),
            0.001f,
        )
    }

    @Test
    fun opacityAt_returnsToMaxOpacityAfterFullCycle() {
        val fullCycle = ChatStreamingTextCursorPolicy.BLINK_HALF_PERIOD_MS * 2
        assertEquals(1f, ChatStreamingTextCursorPolicy.opacityAt(fullCycle), 0.001f)
    }

    @Test
    fun shouldUpdateCursorAttributesOnly_whenTextUnchangedAndOpacityChanged() {
        assertTrue(
            ChatStreamingTextCursorPolicy.shouldUpdateCursorAttributesOnly(
                appliedText = "Hello",
                newText = "Hello",
                appliedShowsCursor = true,
                showsCursor = true,
                appliedCursorOpacity = 1f,
                newCursorOpacity = 0.5f,
            ),
        )
    }

    @Test
    fun shouldUpdateCursorAttributesOnly_isFalseWhenTextChanged() {
        assertFalse(
            ChatStreamingTextCursorPolicy.shouldUpdateCursorAttributesOnly(
                appliedText = "Hello",
                newText = "Hello world",
                appliedShowsCursor = true,
                showsCursor = true,
                appliedCursorOpacity = 1f,
                newCursorOpacity = 0.5f,
            ),
        )
    }

    @Test
    fun shouldUpdateCursorAttributesOnly_isFalseWhenCursorHidden() {
        assertFalse(
            ChatStreamingTextCursorPolicy.shouldUpdateCursorAttributesOnly(
                appliedText = "Hello",
                newText = "Hello",
                appliedShowsCursor = false,
                showsCursor = false,
                appliedCursorOpacity = 1f,
                newCursorOpacity = 0.5f,
            ),
        )
    }

    @Test
    fun shouldUpdateCursorAttributesOnly_isFalseWhenOpacityUnchanged() {
        assertFalse(
            ChatStreamingTextCursorPolicy.shouldUpdateCursorAttributesOnly(
                appliedText = "Hello",
                newText = "Hello",
                appliedShowsCursor = true,
                showsCursor = true,
                appliedCursorOpacity = 0.8f,
                newCursorOpacity = 0.8f,
            ),
        )
    }

    @Test
    fun shouldRebuild_whenCursorVisibilityChanges() {
        assertTrue(
            ChatStreamingTextCursorPolicy.shouldRebuild(
                appliedText = "Hello",
                newText = "Hello",
                appliedShowsCursor = false,
                showsCursor = true,
                appliedCursorOpacity = 1f,
                newCursorOpacity = 1f,
            ),
        )
    }

    @Test
    fun shouldRebuild_whenTextChanges() {
        assertTrue(
            ChatStreamingTextCursorPolicy.shouldRebuild(
                appliedText = "Hello",
                newText = "Hello!",
                appliedShowsCursor = true,
                showsCursor = true,
                appliedCursorOpacity = 1f,
                newCursorOpacity = 1f,
            ),
        )
    }

    @Test
    fun shouldRebuild_isFalseWhenNothingChanged() {
        assertFalse(
            ChatStreamingTextCursorPolicy.shouldRebuild(
                appliedText = "Hello",
                newText = "Hello",
                appliedShowsCursor = true,
                showsCursor = true,
                appliedCursorOpacity = 0.6f,
                newCursorOpacity = 0.6f,
            ),
        )
    }
}
