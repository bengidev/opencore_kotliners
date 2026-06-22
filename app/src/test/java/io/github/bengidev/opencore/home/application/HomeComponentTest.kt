package io.github.bengidev.opencore.home.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
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
            onSendMessage = { sent = true }
        )
        advanceUntilIdle()

        component.onDraftMessageChanged("   ")
        component.onSendTapped()

        assertFalse(sent)
    }

    @Test
    fun modelSelected_persistsSelection() = runTest(testDispatcher) {
        val store = InMemorySidePanelPreferenceStore()
        val lifecycle = LifecycleRegistry().apply { resume() }
        val component = HomeComponent(
            componentContext = DefaultComponentContext(lifecycle),
            preferenceStore = store
        )
        advanceUntilIdle()

        val model = component.state.value.availableModels.first()
        component.onModelSelected(model)
        advanceUntilIdle()

        assertEquals(model.id, store.preference().modelId)
        assertEquals(model.displayTitle, component.state.value.selectedModelTitle)
    }
}
