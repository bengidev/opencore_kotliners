package io.github.bengidev.opencore.chat.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatStreamingMarkdownSafeTextTest {
    @Test
    fun sanitize_hidesUnclosedBoldUntilClosingMarkerArrives() {
        assertEquals("Hello ", ChatStreamingMarkdownSafeText.sanitize("Hello **bol"))
        assertEquals("Hello ", ChatStreamingMarkdownSafeText.sanitize("Hello **bold"))
        assertEquals("Hello **bold**", ChatStreamingMarkdownSafeText.sanitize("Hello **bold**"))
    }

    @Test
    fun sanitize_hidesUnclosedInlineCode() {
        assertEquals("Use ", ChatStreamingMarkdownSafeText.sanitize("Use `partial"))
        assertEquals("Use `code`", ChatStreamingMarkdownSafeText.sanitize("Use `code`"))
    }

    @Test
    fun sanitize_hidesUnclosedCodeFence() {
        assertEquals("Intro\n\n", ChatStreamingMarkdownSafeText.sanitize("Intro\n\n```kotlin\nfun main()"))
    }
}
