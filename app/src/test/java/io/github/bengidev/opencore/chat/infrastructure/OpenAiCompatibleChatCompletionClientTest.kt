package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import java.time.Instant
import java.util.UUID

class OpenAiCompatibleChatCompletionClientTest {

    @Test
    fun complete_unknownHost_returnsFriendlyMessage() = runTest {
        val client = OpenAiCompatibleChatCompletionClient { _, _, _ ->
            throw UnknownHostException("Unable to resolve host \"openrouter.ai\"")
        }

        val reply = client.complete(
            provider = SidePanelProviderApi.openRouter,
            modelId = "openrouter/free",
            apiKey = "sk-test",
            messages = sampleMessages(),
            reasoning = SidePanelReasoningModel.Off
        )

        assertEquals(ChatMessageRole.ASSISTANT, reply.role)
        assertTrue(reply.content.contains("openrouter.ai"))
        assertTrue(reply.content.contains("DNS lookup failed"))
    }

    private fun sampleMessages(): List<SidePanelMessage> = listOf(
        SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = "hello",
            createdAt = Instant.now()
        )
    )
}
