package io.github.bengidev.opencore.shared.persistence.session

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.shared.persistence.session.AtomCompactionCheckpoint
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class AtomSessionContextBuilderTest {
    @Test
    fun buildModelMessages_appliesLatestCompactionCheckpoint() {
        val atomId = UUID.randomUUID()
        val keptId = UUID.randomUUID()
        val droppedId = UUID.randomUUID()
        val compactionId = UUID.randomUUID()

        val dropped = AtomSessionEntry.messageEntry(
            id = droppedId,
            atomId = atomId,
            parentId = null,
            message = SidePanelMessage(
                id = droppedId,
                role = ChatMessageRole.USER,
                content = "old",
                createdAt = Instant.EPOCH,
            ),
            timestamp = Instant.EPOCH,
        )
        val kept = AtomSessionEntry.messageEntry(
            id = keptId,
            atomId = atomId,
            parentId = droppedId,
            message = SidePanelMessage(
                id = keptId,
                role = ChatMessageRole.USER,
                content = "recent",
                createdAt = Instant.ofEpochSecond(10),
            ),
            timestamp = Instant.ofEpochSecond(10),
        )
        val compaction = AtomSessionEntry.compactionEntry(
            id = compactionId,
            atomId = atomId,
            parentId = keptId,
            checkpoint = AtomCompactionCheckpoint(
                summary = "summary body",
                firstKeptEntryId = keptId,
                tokensBefore = 100,
            ),
            timestamp = Instant.ofEpochSecond(20),
        )

        val projected = AtomSessionContextBuilder.buildModelMessages(
            entries = listOf(dropped, kept, compaction),
            leafId = compactionId,
        )

        assertEquals(2, projected.size)
        assertTrue(projected.first().content.contains("summary body"))
        assertEquals("recent", projected.last().content)
    }
}
