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
                previousMessageCount = 0,
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
                previousMessageCount = 3,
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
                previousMessageCount = 0,
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
                previousMessageCount = 0,
            )
        )
    }

    @Test
    fun shouldAnimateScroll_falseForFollowUpTurn() {
        assertFalse(
            ChatThreadScrollPolicy.shouldAnimateScroll(
                isBulkRestore = false,
                streamingRevision = 0,
                imeVisible = false,
                previousMessageCount = 3,
            )
        )
    }

    @Test
    fun shouldAnimateReasoningCollapseScroll_neverAnimates() {
        assertFalse(ChatThreadScrollPolicy.shouldAnimateReasoningCollapseScroll())
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

    @Test
    fun shouldScrollForNewMessage_whenCountIncreases() {
        assertTrue(
            ChatThreadScrollPolicy.shouldScrollForNewMessage(
                previousMessageCount = 2,
                messageCount = 3,
            )
        )
        assertFalse(
            ChatThreadScrollPolicy.shouldScrollForNewMessage(
                previousMessageCount = 3,
                messageCount = 3,
            )
        )
    }

    @Test
    fun shouldScrollForImeChange_whenInsetChanges() {
        assertTrue(
            ChatThreadScrollPolicy.shouldScrollForImeChange(
                imeBottomPx = 480,
                previousImeBottomPx = 0,
            )
        )
        assertTrue(
            ChatThreadScrollPolicy.shouldScrollForImeChange(
                imeBottomPx = 520,
                previousImeBottomPx = 480,
            )
        )
        assertFalse(
            ChatThreadScrollPolicy.shouldScrollForImeChange(
                imeBottomPx = 480,
                previousImeBottomPx = 480,
            )
        )
    }

    @Test
    fun shouldScrollForStreamFinished_whenSendingStops() {
        assertTrue(
            ChatThreadScrollPolicy.shouldScrollForStreamFinished(
                wasSending = true,
                isSending = false,
            )
        )
        assertFalse(
            ChatThreadScrollPolicy.shouldScrollForStreamFinished(
                wasSending = false,
                isSending = false,
            )
        )
        assertFalse(
            ChatThreadScrollPolicy.shouldScrollForStreamFinished(
                wasSending = true,
                isSending = true,
            )
        )
    }

    @Test
    fun shouldScrollForStreamingRevision_whenRevisionIsPositive() {
        assertTrue(ChatThreadScrollPolicy.shouldScrollForStreamingRevision(streamingRevision = 1))
        assertFalse(ChatThreadScrollPolicy.shouldScrollForStreamingRevision(streamingRevision = 0))
    }
}
