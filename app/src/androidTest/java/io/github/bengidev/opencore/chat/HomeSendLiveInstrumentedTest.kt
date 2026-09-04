package io.github.bengidev.opencore.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.atoms.AtomsFacade
import io.github.bengidev.opencore.atoms.application.AtomsComponent
import io.github.bengidev.opencore.home.HomeFacade
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.shared.credential.CredentialEncryptedStore
import io.github.bengidev.opencore.shared.persistence.room.RoomAtomHistoryRepository
import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSettingsContextCompactionPreferenceStore
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mirrors [MainActivity] wiring: HomeComponent → chatComponentHolder → ChatComponent.
 */
@RunWith(AndroidJUnit4::class)
class HomeSendLiveInstrumentedTest {

    @Test
    fun homeOnSendTapped_receivesAssistantReply() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var chatComponent: ChatComponent
        lateinit var homeComponent: HomeComponent

        instrumentation.runOnMainSync {
            runBlocking {
                val context = instrumentation.targetContext
                val credentialStore = CredentialEncryptedStore(context)
                val apiKey = credentialStore.secret(ProviderDescriptor.openRouter.id)?.trim()
                assumeTrue("Save an OpenRouter API key in Settings before running this test.", !apiKey.isNullOrBlank())

                val preferenceStore = DataStoreSidePanelPreferenceStore(context)
                val preference = preferenceStore.preference()
                assumeTrue("Select a model in the app before running this test.", !preference.modelId.isNullOrBlank())

                val history = RoomAtomHistoryRepository(context.applicationContext)
                val atomsComponent: AtomsComponent = AtomsFacade().createComponent(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    history = history,
                )
                val holder = arrayOfNulls<ChatComponent>(1)
                homeComponent = HomeFacade().createComponent(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    preferenceStore = preferenceStore,
                    credentialStore = credentialStore,
                    onSendMessage = { message, providerSortBy, reasoningEffort ->
                        holder[0]?.sendUserMessage(message, providerSortBy, reasoningEffort)
                    },
                )
                chatComponent = ChatFacade().createComponent(
                    componentContext = DefaultComponentContext(LifecycleRegistry()),
                    history = history,
                    preferenceStore = preferenceStore,
                    credentialStore = credentialStore,
                    compactionPreferenceStore = DataStoreSettingsContextCompactionPreferenceStore(context),
                    contextLengthProvider = {
                        val state = homeComponent.state.value
                        state.selectedModelId?.let { id ->
                            state.availableModels.firstOrNull { it.id == id }?.contextLength
                        }
                    },
                )
                holder[0] = chatComponent
                chatComponent.onHistoryChanged = { atomsComponent.refreshIfNeeded() }

                homeComponent.onDraftMessageChanged("Reply with exactly one word: pong")
                homeComponent.onCredentialsChanged()
                for (attempt in 0 until 80) {
                    if (homeComponent.state.value.canSendBase) break
                    Thread.sleep(250)
                }
                val preSend = homeComponent.state.value
                assumeTrue(
                    "Home canSendBase never became true. state=$preSend",
                    preSend.canSendBase,
                )

                homeComponent.onSendTapped()
                val afterSend = chatComponent.state.value
                assumeTrue(
                    "sendUserMessage did not append a user row. homeDraft=${preSend.draftMessage} chat=$afterSend",
                    afterSend.messages.any { it.role == ChatMessageRole.USER },
                )
            }
        }

        var sawSending = false
        repeat(120) {
            instrumentation.waitForIdleSync()
            val state = chatComponent.state.value
            if (state.isSending) sawSending = true
            if (!state.isSending) {
                if (!sawSending && it < 20) {
                    Thread.sleep(500)
                    return@repeat
                }
                if (state.streamingStatus == ChatStreamingStatus.Failed) {
                    throw AssertionError("Chat failed: ${state.streamErrorMessage}")
                }
                val assistantMessages = state.messages.filter { it.role == ChatMessageRole.ASSISTANT }
                assertFalse(
                    "Expected assistant reply via Home wiring, status=${state.streamingStatus} " +
                        "error=${state.streamErrorMessage} messages=${state.messages}",
                    assistantMessages.isEmpty(),
                )
                assertTrue(assistantMessages.any { it.content.isNotBlank() })
                return
            }
            Thread.sleep(500)
        }

        val finalState = chatComponent.state.value
        throw AssertionError(
            "Timed out. isSending=${finalState.isSending} status=${finalState.streamingStatus} " +
                "error=${finalState.streamErrorMessage} messages=${finalState.messages}",
        )
    }
}
