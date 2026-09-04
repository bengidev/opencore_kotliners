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

    @Test
    fun buildThreadDisplayMessages_placesSummaryAfterKeptTurnAndHidesSummarizedMessages() {
        val atomId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val assistantId = UUID.randomUUID()
        val compactionId = UUID.randomUUID()

        val user = AtomSessionEntry.messageEntry(
            id = userId,
            atomId = atomId,
            parentId = null,
            message = SidePanelMessage(
                id = userId,
                role = ChatMessageRole.USER,
                content = "where u from?",
                createdAt = Instant.ofEpochSecond(10),
            ),
            timestamp = Instant.ofEpochSecond(10),
        )
        val assistant = AtomSessionEntry.messageEntry(
            id = assistantId,
            atomId = atomId,
            parentId = userId,
            message = SidePanelMessage(
                id = assistantId,
                role = ChatMessageRole.ASSISTANT,
                content = "I am an AI model.",
                createdAt = Instant.ofEpochSecond(20),
            ),
            timestamp = Instant.ofEpochSecond(20),
        )
        val compaction = AtomSessionEntry.compactionEntry(
            id = compactionId,
            atomId = atomId,
            parentId = assistantId,
            checkpoint = AtomCompactionCheckpoint(
                summary = "summary body",
                firstKeptEntryId = assistantId,
                tokensBefore = 100,
            ),
            timestamp = Instant.ofEpochSecond(30),
        )

        val thread = AtomSessionContextBuilder.buildThreadDisplayMessages(
            entries = listOf(user, assistant, compaction),
            leafId = compactionId,
        )

        assertEquals(2, thread.size)
        assertEquals("I am an AI model.", thread.first().content)
        assertTrue(thread.last().content.contains("summary body"))
    }

    @Test
    fun buildThreadDisplayMessages_placesSummaryAfterFullTurnWhenUserMessageIsKept() {
        val atomId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val assistantId = UUID.randomUUID()
        val compactionId = UUID.randomUUID()

        val user = AtomSessionEntry.messageEntry(
            id = userId,
            atomId = atomId,
            parentId = null,
            message = SidePanelMessage(
                id = userId,
                role = ChatMessageRole.USER,
                content = "tell me about what is ai assistant?",
                createdAt = Instant.ofEpochSecond(10),
            ),
            timestamp = Instant.ofEpochSecond(10),
        )
        val assistant = AtomSessionEntry.messageEntry(
            id = assistantId,
            atomId = atomId,
            parentId = userId,
            message = SidePanelMessage(
                id = assistantId,
                role = ChatMessageRole.ASSISTANT,
                content = "What Is an AI Assistant?",
                createdAt = Instant.ofEpochSecond(20),
            ),
            timestamp = Instant.ofEpochSecond(20),
        )
        val compaction = AtomSessionEntry.compactionEntry(
            id = compactionId,
            atomId = atomId,
            parentId = assistantId,
            checkpoint = AtomCompactionCheckpoint(
                summary = "summary body",
                firstKeptEntryId = userId,
                tokensBefore = 100,
            ),
            timestamp = Instant.ofEpochSecond(30),
        )

        val thread = AtomSessionContextBuilder.buildThreadDisplayMessages(
            entries = listOf(user, assistant, compaction),
            leafId = compactionId,
        )

        assertEquals(3, thread.size)
        assertEquals("tell me about what is ai assistant?", thread[0].content)
        assertEquals("What Is an AI Assistant?", thread[1].content)
        assertTrue(thread[2].content.contains("summary body"))
    }

    @Test
    fun buildThreadDisplayMessages_keepsRecentTurnBeforeSummary() {
        val atomId = UUID.randomUUID()
        val oldId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val assistantId = UUID.randomUUID()
        val compactionId = UUID.randomUUID()

        val old = AtomSessionEntry.messageEntry(
            id = oldId,
            atomId = atomId,
            parentId = null,
            message = SidePanelMessage(
                id = oldId,
                role = ChatMessageRole.USER,
                content = "old",
                createdAt = Instant.EPOCH,
            ),
            timestamp = Instant.EPOCH,
        )
        val user = AtomSessionEntry.messageEntry(
            id = userId,
            atomId = atomId,
            parentId = oldId,
            message = SidePanelMessage(
                id = userId,
                role = ChatMessageRole.USER,
                content = "recent question",
                createdAt = Instant.ofEpochSecond(10),
            ),
            timestamp = Instant.ofEpochSecond(10),
        )
        val assistant = AtomSessionEntry.messageEntry(
            id = assistantId,
            atomId = atomId,
            parentId = userId,
            message = SidePanelMessage(
                id = assistantId,
                role = ChatMessageRole.ASSISTANT,
                content = "recent answer",
                createdAt = Instant.ofEpochSecond(20),
            ),
            timestamp = Instant.ofEpochSecond(20),
        )
        val compaction = AtomSessionEntry.compactionEntry(
            id = compactionId,
            atomId = atomId,
            parentId = assistantId,
            checkpoint = AtomCompactionCheckpoint(
                summary = "summary body",
                firstKeptEntryId = userId,
                tokensBefore = 100,
            ),
            timestamp = Instant.ofEpochSecond(30),
        )

        val thread = AtomSessionContextBuilder.buildThreadDisplayMessages(
            entries = listOf(old, user, assistant, compaction),
            leafId = compactionId,
        )

        assertEquals(3, thread.size)
        assertEquals("recent question", thread[0].content)
        assertEquals("recent answer", thread[1].content)
        assertTrue(thread[2].content.contains("summary body"))
    }
}
