package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamingStatusCapsuleTest {

    @Test
    fun showsStreamingStatusCapsule_whenSendingAndRunning() {
        val state = ChatState(isSending = true, streamingStatus = ChatStreamingStatus.Running)
        assertTrue(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenWhenNotSending() {
        val state = ChatState(isSending = false, streamingStatus = ChatStreamingStatus.Running)
        assertFalse(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenWhenStreamingIdle() {
        val state = ChatState(isSending = true, streamingStatus = ChatStreamingStatus.Idle)
        assertFalse(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenWhenNotSendingAndIdle() {
        val state = ChatState(isSending = false, streamingStatus = ChatStreamingStatus.Idle)
        assertFalse(state.showsStreamingStatusCapsule)
    }
}
