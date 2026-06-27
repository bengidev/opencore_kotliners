package io.github.bengidev.opencore.chat.presenter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatThreadScrollPolicyTest {

    @Test
    fun shouldAnimateScroll_falseForHistoryRestore() {
        assertFalse(
            ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = true,
                streamingRevision = 0,
                imeVisible = false,
            )
        )
    }

    @Test
    fun shouldAnimateScroll_falseWhileStreaming() {
        assertFalse(
            ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = false,
                streamingRevision = 2,
                imeVisible = false,
            )
        )
    }

    @Test
    fun shouldAnimateScroll_falseWhenKeyboardVisible() {
        assertFalse(
            ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = false,
                streamingRevision = 0,
                imeVisible = true,
            )
        )
    }

    @Test
    fun shouldAnimateScroll_trueForFreshUserMessage() {
        assertTrue(
            ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = false,
                streamingRevision = 0,
                imeVisible = false,
            )
        )
    }

    @Test
    fun shouldDeferForActiveScroll_whenUserIsDragging() {
        assertTrue(ChatThreadScrollPolicy.shouldDeferForActiveScroll(isScrollInProgress = true))
        assertFalse(ChatThreadScrollPolicy.shouldDeferForActiveScroll(isScrollInProgress = false))
    }

    @Test
    fun isBulkRestore_detectsHistoryLoad() {
        assertTrue(ChatThreadScrollPolicy.isBulkRestore(previousMessageCount = 0, messageCount = 4))
        assertFalse(ChatThreadScrollPolicy.isBulkRestore(previousMessageCount = 3, messageCount = 4))
    }
}
