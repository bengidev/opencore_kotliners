package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelPreferenceStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ProviderChatCompletionClientTest {

    @Test
    fun complete_withoutApiKey_promptsToAddKey() = runTest {
        val client = ProviderChatCompletionClient(
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(
                    providerId = SidePanelProviderApi.openRouter.id,
                    modelId = "openrouter/free"
                )
            ),
            credentialStore = InMemorySidePanelCredentialStore()
        )

        val reply = client.complete(sampleMessages())

        assertEquals(ChatMessageRole.ASSISTANT, reply.role)
        assertTrue(reply.content.contains("API key"))
    }

    @Test
    fun complete_withApiKey_delegatesToProvider() = runTest {
        val credentialStore = InMemorySidePanelCredentialStore().apply {
            save("sk-test", SidePanelProviderApi.openRouter.id)
        }
        val delegate = OpenAiCompatibleChatCompletionClient { _, _, _ ->
            """{"choices":[{"message":{"role":"assistant","content":"Real reply"}}]}"""
        }
        val client = ProviderChatCompletionClient(
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(
                    providerId = SidePanelProviderApi.openRouter.id,
                    modelId = "openrouter/free",
                    reasoningModel = SidePanelReasoningModel.Off
                )
            ),
            credentialStore = credentialStore,
            delegate = delegate
        )

        val reply = client.complete(sampleMessages())

        assertEquals("Real reply", reply.content)
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
