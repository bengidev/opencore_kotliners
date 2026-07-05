package io.github.bengidev.opencore.chat.presenter

import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ChatCompetingStreamPolicyTest {
    @Test
    fun hasCompetingStream_whenStreamingAnswerIdSet_returnsTrue() {
        val state = ChatState(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            streamingAnswerId = UUID.randomUUID(),
        )

        assertTrue(ChatCompetingStreamPolicy.hasCompetingStream(state))
    }

    @Test
    fun hasCompetingStream_whenBufferedAnswerText_returnsTrue() {
        val state = ChatState(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            currentPartialText = "Hello",
        )

        assertTrue(ChatCompetingStreamPolicy.hasCompetingStream(state))
    }

    @Test
    fun hasCompetingStream_whenIncompleteAssistantAnswerRow_returnsTrue() {
        val answerId = UUID.randomUUID()
        val state = ChatState(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            messages = listOf(
                assistantMessage(answerId, "Partial answer", isComplete = false),
            ),
        )

        assertTrue(ChatCompetingStreamPolicy.hasCompetingStream(state))
    }

    @Test
    fun hasCompetingStream_whenOnlyThinkingStreams_returnsFalse() {
        val state = ChatState(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            streamingThinkingId = UUID.randomUUID(),
            currentPartialThinking = "Still thinking",
            messages = listOf(
                assistantMessage(UUID.randomUUID(), "Thinking", kind = SidePanelMessageKind.THINKING, isComplete = false),
            ),
        )

        assertFalse(ChatCompetingStreamPolicy.hasCompetingStream(state))
    }

    private fun assistantMessage(
        id: UUID,
        content: String,
        kind: SidePanelMessageKind = SidePanelMessageKind.TEXT,
        isComplete: Boolean,
    ) = SidePanelMessage(
        id = id,
        role = ChatMessageRole.ASSISTANT,
        content = content,
        createdAt = Instant.EPOCH,
        kind = kind,
        isComplete = isComplete,
    )
}
