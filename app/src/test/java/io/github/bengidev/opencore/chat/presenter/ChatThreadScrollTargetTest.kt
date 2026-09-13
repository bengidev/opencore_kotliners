package io.github.bengidev.opencore.chat.presenter

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatThreadScrollTargetTest {
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val assistantId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val followUpUserId = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val thinkingId = UUID.fromString("00000000-0000-0000-0000-000000000004")
    private val answerId = UUID.fromString("00000000-0000-0000-0000-000000000005")
    private val outputStreamId = UUID.fromString("00000000-0000-0000-0000-000000000006")
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun scrollIndex_followUpUserMessage_scrollsToNewBubble() {
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "First"),
            message(assistantId, ChatMessageRole.ASSISTANT, "Reply"),
            message(followUpUserId, ChatMessageRole.USER, "Second"),
        )

        assertEquals(2, ChatThreadScrollTarget.scrollIndex(messages))
    }

    @Test
    fun scrollIndex_prefersAssistantAnswerOverThinking() {
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "Question"),
            message(thinkingId, ChatMessageRole.ASSISTANT, "Reasoning…", SidePanelMessageKind.THINKING),
            message(answerId, ChatMessageRole.ASSISTANT, "Answer", isComplete = false),
        )

        assertEquals(2, ChatThreadScrollTarget.scrollIndex(messages))
    }

    @Test
    fun scrollIndex_prefersOutputStreamOverThinking() {
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "Question"),
            message(thinkingId, ChatMessageRole.ASSISTANT, "Reasoning…", SidePanelMessageKind.THINKING),
            message(
                outputStreamId,
                ChatMessageRole.ASSISTANT,
                "",
                SidePanelMessageKind.OUTPUT_STREAM,
            ),
        )

        assertEquals(2, ChatThreadScrollTarget.scrollIndex(messages))
    }

    @Test
    fun scrollIndex_thinkingOnlyTurn_scrollsToThinkingCard() {
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "Question"),
            message(
                thinkingId,
                ChatMessageRole.ASSISTANT,
                "Reasoning…",
                SidePanelMessageKind.THINKING,
                isComplete = false,
            ),
        )

        assertEquals(1, ChatThreadScrollTarget.scrollIndex(messages))
    }

    @Test
    fun scrollIndex_completedAssistantReply_scrollsToAssistantText() {
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "Question"),
            message(assistantId, ChatMessageRole.ASSISTANT, "Reply"),
        )

        assertEquals(1, ChatThreadScrollTarget.scrollIndex(messages))
    }

    private fun message(
        id: UUID,
        role: String,
        content: String,
        kind: SidePanelMessageKind = SidePanelMessageKind.TEXT,
        isComplete: Boolean = true,
    ): SidePanelMessage =
        SidePanelMessage(
            id = id,
            role = role,
            content = content,
            createdAt = now,
            kind = kind,
            isComplete = isComplete,
        )
}
