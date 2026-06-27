package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.EchoChatStreamingClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryRestoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var history: InMemorySidePanelHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        history = InMemorySidePanelHistoryRepository(seed = emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun messagesLoaded_replacesThreadContent() {
        val id = UUID.randomUUID()
        val loaded = listOf(
            message(ChatMessageRole.USER, "Hi"),
            message(ChatMessageRole.ASSISTANT, "Hello"),
        )
        val result = ChatReducer.reduce(
            ChatState(activeConversation = conversation().copy(id = id), isLoadingMessages = true),
            ChatIntent.MessagesLoaded(id, loaded),
        )
        assertEquals(2, result.messages.size)
    }

    @Test
    fun newConversation_resetsStreamingRevision() {
        val result = ChatReducer.reduce(
            ChatState(streamingRevision = 3, activeConversation = conversation()),
            ChatIntent.NewConversation,
        )
        assertTrue(result.messages.isEmpty())
        assertEquals(0, result.streamingRevision)
    }

    @Test
    fun reopenConversation_loadsHistoryMessages() = runTest(testDispatcher) {
        val conversation = SidePanelConversation(title = "Test")
        history.saveConversation(conversation)
        listOf(
            message(ChatMessageRole.USER, "Earlier"),
            message(ChatMessageRole.ASSISTANT, "Reply"),
        ).forEach { history.appendMessage(conversation.id, it) }
        val component = ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
            history = history,
            streamingClient = EchoChatStreamingClient(),
        )

        component.openConversation(conversation)
        advanceUntilIdle()

        assertEquals(2, component.state.value.messages.size)
        assertEquals(0, component.state.value.streamingRevision)
    }

    private fun conversation(title: String = "Test") = SidePanelConversation(
        id = UUID.randomUUID(),
        title = title,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )

    private fun message(role: String, content: String) = SidePanelMessage(
        id = UUID.randomUUID(),
        role = role,
        content = content,
        createdAt = Instant.parse("2024-01-01T00:01:00Z"),
        isComplete = true,
    )
}
