package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.infrastructure.EchoChatStreamingClient
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStreamingStatusCapsuleTest {

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
    fun showsStreamingStatusCapsule_whenSendingAndRunning() {
        val state = ChatState(isSending = true, streamingStatus = ChatStreamingStatus.Running)
        assertTrue(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenWhenNotSending() {
        val state = ChatState(isSending = false, streamingStatus = ChatStreamingStatus.Running)
        assertFalse(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenWhenStreamingIdle() {
        val state = ChatState(isSending = true, streamingStatus = ChatStreamingStatus.Idle)
        assertFalse(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenWhenNotSendingAndIdle() {
        val state = ChatState(isSending = false, streamingStatus = ChatStreamingStatus.Idle)
        assertFalse(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenAfterStreamCompletes() = runTest(testDispatcher) {
        val component = ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
            history = InMemorySidePanelHistoryRepository(seed = emptyList()),
            streamingClient = EchoChatStreamingClient(),
        )
        component.sendUserMessage("Question")
        advanceUntilIdle()
        assertFalse(component.state.value.showsStreamingStatusCapsule)
    }
}
