package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.Instant
import java.util.UUID
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
    fun showsStreamingStatusCapsule_whenSendingAndRunningWithOutgoingUserTurn() {
        val state = ChatState(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.USER,
                    content = "Question",
                    createdAt = Instant.now(),
                ),
            ),
        )
        assertTrue(state.showsStreamingStatusCapsule)
    }

    @Test
    fun showsStreamingStatusCapsule_hiddenUntilOutgoingUserTurnIsAnchored() {
        val state = ChatState(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            messages = listOf(
                SidePanelMessage(
                    id = UUID.randomUUID(),
                    role = ChatMessageRole.ASSISTANT,
                    content = "Earlier reply",
                    createdAt = Instant.now(),
                ),
            ),
        )
        assertFalse(state.showsStreamingStatusCapsule)
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
