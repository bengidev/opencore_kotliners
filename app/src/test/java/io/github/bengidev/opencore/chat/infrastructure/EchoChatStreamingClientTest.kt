package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class EchoChatStreamingClientTest {

    @Test
    fun stream_emitsThinkingThenTextDeltas() = runTest {
        val client = EchoChatStreamingClient()
        val messages = listOf(
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.USER,
                content = "Explain coroutines",
                createdAt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )

        val events = client.stream(messages, providerSortBy = null).toList()

        assertTrue(events[0] is ChatStreamingEvent.ThinkingDelta)
        assertEquals(
            "Thinking about: Explain coroutines",
            (events[0] as ChatStreamingEvent.ThinkingDelta).text
        )
        assertEquals(ChatStreamingEvent.TextDelta("Echo: Explain coroutines"), events[1])
        assertEquals(ChatStreamingEvent.Done, events[2])
    }
}
