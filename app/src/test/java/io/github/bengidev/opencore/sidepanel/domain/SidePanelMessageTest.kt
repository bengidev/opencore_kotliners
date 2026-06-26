package io.github.bengidev.opencore.sidepanel.domain

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class SidePanelMessageTest {

    private val now = Instant.parse("2024-01-01T00:00:00Z")

    private fun message(id: UUID, content: String) = SidePanelMessage(
        id = id,
        role = ChatMessageRole.USER,
        content = content,
        createdAt = now,
    )

    @Test
    fun dedupeByMessageId_keepsLatestDuplicate() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val messages = listOf(
            message(id, "older"),
            message(UUID.randomUUID(), "unique"),
            message(id, "newer"),
        )
        val deduped = messages.dedupeByMessageId()
        assertEquals(2, deduped.size)
        assertEquals("unique", deduped[0].content)
        assertEquals("newer", deduped[1].content)
    }

    @Test
    fun dedupeByMessageId_preservesOrderWhenNoDuplicates() {
        val first = message(UUID.randomUUID(), "a")
        val second = message(UUID.randomUUID(), "b")
        val deduped = listOf(first, second).dedupeByMessageId()
        assertEquals(listOf(first, second), deduped)
    }
}
