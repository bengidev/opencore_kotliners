package io.github.bengidev.opencore.chat.presenter

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ChatThreadItemKeyPolicyTest {

    private val now = Instant.parse("2024-01-01T00:00:00Z")

    private fun message(
        id: UUID,
        kind: SidePanelMessageKind = SidePanelMessageKind.TEXT,
    ) = SidePanelMessage(
        id = id,
        role = ChatMessageRole.ASSISTANT,
        content = "Body",
        createdAt = now,
        kind = kind,
    )

    @Test
    fun keysFor_sameIdDifferentKinds_areDistinct() {
        val id = UUID.randomUUID()
        val keys = ChatThreadItemKeyPolicy.keysFor(
            listOf(
                message(id, SidePanelMessageKind.THINKING),
                message(id, SidePanelMessageKind.TEXT),
            )
        )
        assertTrue(ChatThreadItemKeyPolicy.hasUniqueKeys(
            listOf(
                message(id, SidePanelMessageKind.THINKING),
                message(id, SidePanelMessageKind.TEXT),
            )
        ))
        assertEquals(2, keys.toSet().size)
    }

    @Test
    fun hasUniqueKeys_falseWhenSameIdAndKindRepeat() {
        val id = UUID.randomUUID()
        val messages = listOf(message(id), message(id))
        assertFalse(ChatThreadItemKeyPolicy.hasUniqueKeys(messages))
    }
}
