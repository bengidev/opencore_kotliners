package io.github.bengidev.opencore.chat.utilities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamingMarkdownGuardTest {
    @Test
    fun shouldUsePlainFallback_completeMarkdown_returnsFalse() {
        assertFalse(
            ChatStreamingMarkdownGuard.shouldUsePlainFallback(
                "Done **bold** and `inline`",
            ),
        )
    }

    @Test
    fun shouldUsePlainFallback_unclosedFence_returnsTrue() {
        assertTrue(ChatStreamingMarkdownGuard.shouldUsePlainFallback("```kotlin\nfun main()"))
    }

    @Test
    fun shouldUsePlainFallback_closedFence_returnsFalse() {
        assertFalse(
            ChatStreamingMarkdownGuard.shouldUsePlainFallback(
                """
                ```kotlin
                fun main() {}
                ```
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun shouldUsePlainFallback_oddInlineBackticks_returnsTrue() {
        assertTrue(ChatStreamingMarkdownGuard.shouldUsePlainFallback("partial `code"))
    }

    @Test
    fun shouldUsePlainFallback_evenInlineBackticks_returnsFalse() {
        assertFalse(ChatStreamingMarkdownGuard.shouldUsePlainFallback("done `code` here"))
    }

    @Test
    fun shouldUsePlainFallback_backticksInsideFence_ignored() {
        assertFalse(
            ChatStreamingMarkdownGuard.shouldUsePlainFallback(
                """
                ```kotlin
                val x = `template`
                ```
                """.trimIndent(),
            ),
        )
    }
}
