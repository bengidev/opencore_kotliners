package io.github.bengidev.opencore.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.atoms.AtomsFacade
import io.github.bengidev.opencore.atoms.application.AtomsComponent
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.shared.credential.CredentialEncryptedStore
import io.github.bengidev.opencore.shared.persistence.room.RoomAtomHistoryRepository
import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSettingsContextCompactionPreferenceStore
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end send through [ChatComponent] with real OpenRouter networking.
 */
@RunWith(AndroidJUnit4::class)
class ChatSendLiveInstrumentedTest {

    @Test
    fun sendUserMessage_receivesAssistantReply() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var component: ChatComponent
        lateinit var preference: SidePanelProviderPreference

        instrumentation.runOnMainSync {
            runBlocking {
                val context = instrumentation.targetContext
                val credentialStore = CredentialEncryptedStore(context)
                val apiKey = credentialStore.secret(ProviderDescriptor.openRouter.id)?.trim()
                assumeTrue("Save an OpenRouter API key in Settings before running this test.", !apiKey.isNullOrBlank())

                val preferenceStore = DataStoreSidePanelPreferenceStore(context)
                preference = preferenceStore.preference()
                assumeTrue("Select a model in the app before running this test.", !preference.modelId.isNullOrBlank())

                val history = RoomAtomHistoryRepository(context.applicationContext)
                val atomsComponent: AtomsComponent = AtomsFacade().createComponent(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    history = history,
                )
                component = ChatFacade().createComponent(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    history = history,
                    preferenceStore = preferenceStore,
                    credentialStore = credentialStore,
                    compactionPreferenceStore = DataStoreSettingsContextCompactionPreferenceStore(context),
                    contextLengthProvider = { 200_000 },
                )
                component.onHistoryChanged = { atomsComponent.refreshIfNeeded() }

                component.sendUserMessage(
                    rawText = "Reply with exactly one word: pong",
                    providerSortBy = null,
                    reasoningEffort = preference.reasoningEffortWireValue,
                )
            }
        }

        var sawSending = false
        repeat(120) {
            instrumentation.waitForIdleSync()
            val state = component.state.value
            if (state.isSending) {
                sawSending = true
            }
            if (!state.isSending) {
                if (!sawSending && it < 10) {
                    Thread.sleep(500)
                    return@repeat
                }
                if (state.streamingStatus == ChatStreamingStatus.Failed) {
                    throw AssertionError("Chat failed: ${state.streamErrorMessage}")
                }
                val assistantMessages = state.messages.filter { it.role == ChatMessageRole.ASSISTANT }
                assertFalse(
                    "Expected an assistant reply, got status=${state.streamingStatus} " +
                        "error=${state.streamErrorMessage} messages=${state.messages}",
                    assistantMessages.isEmpty(),
                )
                assertTrue(assistantMessages.any { it.content.isNotBlank() })
                return
            }
            Thread.sleep(500)
        }

        val finalState = component.state.value
        throw AssertionError(
            "Timed out waiting for chat response. isSending=${finalState.isSending} " +
                "status=${finalState.streamingStatus} error=${finalState.streamErrorMessage} " +
                "messages=${finalState.messages.map { it.role + ":" + it.content }}"
        )
    }
}
