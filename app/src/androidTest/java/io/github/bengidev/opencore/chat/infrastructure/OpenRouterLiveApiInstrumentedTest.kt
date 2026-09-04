package io.github.bengidev.opencore.chat.infrastructure

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.shared.credential.CredentialEncryptedStore
import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Hits the real OpenRouter streaming API using credentials stored on the device/emulator.
 * Skips when no API key is saved in Settings.
 */
@RunWith(AndroidJUnit4::class)
class OpenRouterLiveApiInstrumentedTest {

    @Test
    fun stream_withStoredCredentials_returnsTextOrExplicitError() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apiKey = CredentialEncryptedStore(context).secret(ProviderDescriptor.openRouter.id)?.trim()
        assumeTrue("Save an OpenRouter API key in Settings before running this test.", !apiKey.isNullOrBlank())

        val preference = DataStoreSidePanelPreferenceStore(context).preference()
        val modelId = preference.modelId ?: "openrouter/free"
        val reasoningEffort = preference.reasoningEffortWireValue

        val userMessage = SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = "Reply with exactly one word: pong",
            createdAt = Instant.now(),
        )

        val client = OpenAiCompatibleStreamingClient()
        val events = client.stream(
            providerId = ProviderDescriptor.openRouter.id,
            modelId = modelId,
            apiKey = apiKey!!,
            messages = listOf(userMessage),
            reasoningEffort = reasoningEffort,
        ).toList()

        val errors = events.filterIsInstance<ChatStreamingEvent.Error>()
        val text = events.filterIsInstance<ChatStreamingEvent.TextDelta>().joinToString("") { it.text }

        if (errors.isNotEmpty()) {
            val message = errors.first().error.message
            throw AssertionError("OpenRouter returned an error for model=$modelId reasoning=$reasoningEffort: $message")
        }

        assertTrue("Expected Done event from OpenRouter stream", events.any { it is ChatStreamingEvent.Done })
        assertFalse(
            "OpenRouter stream completed without assistant text. Events=$events",
            text.isBlank(),
        )
    }

    @Test
    fun stream_withoutReasoning_returnsTextOrExplicitError() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apiKey = CredentialEncryptedStore(context).secret(ProviderDescriptor.openRouter.id)?.trim()
        assumeTrue("Save an OpenRouter API key in Settings before running this test.", !apiKey.isNullOrBlank())

        val preference = DataStoreSidePanelPreferenceStore(context).preference()
        val modelId = preference.modelId ?: "openrouter/free"

        val userMessage = SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = "Reply with exactly one word: pong",
            createdAt = Instant.now(),
        )

        val client = OpenAiCompatibleStreamingClient()
        val events = client.stream(
            providerId = ProviderDescriptor.openRouter.id,
            modelId = modelId,
            apiKey = apiKey!!,
            messages = listOf(userMessage),
            reasoningEffort = null,
        ).toList()

        val errors = events.filterIsInstance<ChatStreamingEvent.Error>()
        val text = events.filterIsInstance<ChatStreamingEvent.TextDelta>().joinToString("") { it.text }

        if (errors.isNotEmpty()) {
            val message = errors.first().error.message
            throw AssertionError("OpenRouter returned an error without reasoning for model=$modelId: $message")
        }

        assertTrue("Expected Done event from OpenRouter stream", events.any { it is ChatStreamingEvent.Done })
        assertFalse(
            "OpenRouter stream completed without assistant text. Events=$events",
            text.isBlank(),
        )
    }
}
