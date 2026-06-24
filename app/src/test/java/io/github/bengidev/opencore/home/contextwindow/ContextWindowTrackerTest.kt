package io.github.bengidev.opencore.home.contextwindow

import io.github.bengidev.opencore.home.contextwindow.core.ContextWindowTracker
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ContextWindowTrackerTest {

    private fun message(withTokenCount: Int): SidePanelMessage {
        val content = "a".repeat(withTokenCount * 4)
        return SidePanelMessage(
            id = UUID.randomUUID(),
            role = "user",
            content = content,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )
    }

    @Test
    fun refreshUpdatesUsageFromMessagesAndLimit() {
        val tracker = ContextWindowTracker()
        val messages = listOf(message(withTokenCount = 1_000))

        tracker.refresh(messages = messages, draft = null, contextLength = 100_000)

        assertEquals(1_000, tracker.usage.tokensUsed)
        assertEquals(100_000, tracker.usage.tokenLimit)
        assertEquals(0.01, tracker.usage.fractionUsed, 0.0)
    }

    @Test
    fun modelChangeUpdatesLimitPreservesMessageLoad() {
        val tracker = ContextWindowTracker()
        val messages = listOf(message(withTokenCount = 1_000))

        tracker.refresh(messages = messages, draft = null, contextLength = 100_000)
        val usedBeforeModelChange = tracker.usage.tokensUsed
        val fractionBeforeModelChange = tracker.usage.fractionUsed

        tracker.refresh(messages = messages, draft = null, contextLength = 200_000)

        assertEquals(usedBeforeModelChange, tracker.usage.tokensUsed)
        assertEquals(200_000, tracker.usage.tokenLimit)
        assertEquals(fractionBeforeModelChange / 2, tracker.usage.fractionUsed, 0.0)
    }
}
