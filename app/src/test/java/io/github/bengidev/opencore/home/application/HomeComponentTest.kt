package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelPreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeComponentTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendTapped_invokesCallbackWithTrimmedDraft() = runTest(testDispatcher) {
        var sent: String? = null
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(modelId = "openrouter/free")
            ),
            credentialStore = InMemorySidePanelCredentialStore().apply {
                save("sk-test", SidePanelProviderApi.openRouter.id)
            },
            onSendMessage = { sent = it }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("  Hello  ")
        component.onSendTapped()

        assertEquals("Hello", sent)
        assertTrue(component.state.value.draftMessage.isEmpty())
    }

    @Test
    fun sendTapped_blankDraft_doesNotInvokeCallback() = runTest(testDispatcher) {
        var sent = false
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(modelId = "openrouter/free")
            ),
            credentialStore = InMemorySidePanelCredentialStore().apply {
                save("sk-test", SidePanelProviderApi.openRouter.id)
            },
            onSendMessage = { sent = true }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("   ")
        component.onSendTapped()

        assertFalse(sent)
    }

    @Test
    fun sendTapped_withoutApiKey_doesNotInvokeCallback() = runTest(testDispatcher) {
        var sent = false
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(modelId = "openrouter/free")
            ),
            credentialStore = InMemorySidePanelCredentialStore(),
            onSendMessage = { sent = true }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("Hello")
        component.onSendTapped()

        assertFalse(sent)
        assertEquals("Hello", component.state.value.draftMessage)
    }

    @Test
    fun onCredentialsChanged_refreshesApiKeyState() = runTest(testDispatcher) {
        val store = InMemorySidePanelCredentialStore()
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            preferenceStore = InMemorySidePanelPreferenceStore(
                SidePanelProviderPreference(modelId = "openrouter/free")
            ),
            credentialStore = store
        )
        advanceUntilIdle()
        assertFalse(component.state.value.hasApiKey)

        store.save("sk-test", SidePanelProviderApi.openRouter.id)
        component.onCredentialsChanged()
        advanceUntilIdle()

        assertTrue(component.state.value.hasApiKey)
    }

    @Test
    fun modelSelected_persistsSelection() = runTest(testDispatcher) {
        val store = InMemorySidePanelPreferenceStore()
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            preferenceStore = store,
            credentialStore = InMemorySidePanelCredentialStore()
        )
        advanceUntilIdle()

        val model = component.state.value.availableModels.first()
        component.onModelSelected(model)
        advanceUntilIdle()

        assertEquals(model.id, store.preference().modelId)
        assertEquals(model.displayTitle, component.state.value.selectedModelTitle)
    }
}
