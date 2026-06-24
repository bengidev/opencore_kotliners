package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelPreferenceStore
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderChatStreamingClientTest {

    @Test
    fun stream_withoutApiKey_emitsError() = runTest {
        val client = ProviderChatStreamingClient(
            preferenceStore = InMemorySidePanelPreferenceStore(),
            credentialStore = InMemorySidePanelCredentialStore()
        )

        val events = client.stream(emptyList(), providerSortBy = null).toList()

        assertTrue(events[0] is ChatStreamingEvent.Error)
        assertEquals(ChatStreamingEvent.Done, events[1])
    }

    @Test
    fun stream_withoutModelId_emitsError() = runTest {
        val client = ProviderChatStreamingClient(
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(modelId = null)
            ),
            credentialStore = InMemorySidePanelCredentialStore().apply {
                save("sk-test", "openrouter")
            }
        )

        val events = client.stream(emptyList(), providerSortBy = null).toList()

        assertTrue(events[0] is ChatStreamingEvent.Error)
        assertEquals(
            "Select a model for OpenRouter before sending.",
            (events[0] as ChatStreamingEvent.Error).error.message
        )
        assertEquals(ChatStreamingEvent.Done, events[1])
    }
}
