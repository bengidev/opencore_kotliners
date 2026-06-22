package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

import java.util.UUID

class EchoChatCompletionClientTest {

    @Test
    fun complete_echoesLastUserMessage() = runTest {
        val client = EchoChatCompletionClient()
        val messages = listOf(
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.ASSISTANT,
                content = "Hi",
                createdAt = Instant.parse("2024-01-01T00:00:00Z")
            ),
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.USER,
                content = "Explain coroutines",
                createdAt = Instant.parse("2024-01-01T00:01:00Z")
            )
        )

        val reply = client.complete(messages)

        assertEquals(ChatMessageRole.ASSISTANT, reply.role)
        assertEquals("Echo: Explain coroutines", reply.content)
    }
}
