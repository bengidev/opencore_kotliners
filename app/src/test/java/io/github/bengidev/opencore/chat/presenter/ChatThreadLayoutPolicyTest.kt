package io.github.bengidev.opencore.chat.presenter

import androidx.compose.ui.Alignment
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatThreadLayoutPolicyTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun tailScrollIndex_targetsAssistantAnswerInCurrentTurn() {
        val userId = UUID.randomUUID()
        val assistantId = UUID.randomUUID()
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "Question"),
            message(assistantId, ChatMessageRole.ASSISTANT, "Answer"),
        )

        assertEquals(-1, ChatThreadLayoutPolicy.tailScrollIndex(emptyList()))
        assertEquals(0, ChatThreadLayoutPolicy.tailScrollIndex(listOf(message(userId, ChatMessageRole.USER, "only"))))
        assertEquals(1, ChatThreadLayoutPolicy.tailScrollIndex(messages))
    }

    @Test
    fun tailScrollIndex_usesDisplayOrderLength() {
        val userId = UUID.randomUUID()
        val assistantId = UUID.randomUUID()
        val messages = listOf(
            message(userId, ChatMessageRole.USER, "1"),
            message(assistantId, ChatMessageRole.ASSISTANT, "2"),
            message(UUID.randomUUID(), ChatMessageRole.USER, "3"),
            message(UUID.randomUUID(), ChatMessageRole.ASSISTANT, "4"),
            message(UUID.randomUUID(), ChatMessageRole.USER, "5"),
        )

        val index = ChatThreadLayoutPolicy.tailScrollIndex(ChatThreadLayoutPolicy.displayOrder(messages))

        assertEquals(4, index)
    }

    @Test
    fun usesBottomBoxAnchor_notReverseLayout() {
        assertFalse(ChatThreadLayoutPolicy.useReverseLayout())
        assertEquals(Alignment.BottomStart, ChatThreadLayoutPolicy.listAlignment)
    }

    @Test
    fun tailScrollOffset_pinsLastRowToViewportBottom() {
        assertEquals(Int.MAX_VALUE, ChatThreadLayoutPolicy.tailScrollOffset())
    }

    private fun message(id: UUID, role: String, content: String): SidePanelMessage =
        SidePanelMessage(
            id = id,
            role = role,
            content = content,
            createdAt = now,
            kind = SidePanelMessageKind.TEXT,
        )
}
