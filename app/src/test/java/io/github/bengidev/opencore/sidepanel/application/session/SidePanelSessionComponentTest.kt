package io.github.bengidev.opencore.sidepanel.application.session

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SidePanelSessionComponentTest {

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
    fun toggleSidebar_loadsSeededConversations() = runTest(testDispatcher) {
        val samples = SidePanelConversation.previewSamples()
        val component = SidePanelSessionComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            history = InMemorySidePanelHistoryRepository(seed = samples)
        )

        component.toggleSidebar()
        advanceUntilIdle()

        assertEquals(samples.size, component.state.value.conversations.size)
    }

    @Test
    fun recordDraftConversation_persistsToHistory() = runTest(testDispatcher) {
        val history = InMemorySidePanelHistoryRepository(seed = emptyList())
        val component = SidePanelSessionComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            history = history
        )

        component.recordDraftConversation("Hello from home")
        advanceUntilIdle()

        assertEquals(1, history.listConversations().size)
        assertEquals("Hello from home", history.listConversations().single().title)
    }
}
