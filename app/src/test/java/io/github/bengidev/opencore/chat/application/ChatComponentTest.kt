package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatCompletionClient
import io.github.bengidev.opencore.chat.infrastructure.EchoChatCompletionClient
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatComponentTest {

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

    private fun createComponent(
        completionClient: ChatCompletionClient = EchoChatCompletionClient()
    ): ChatComponent {
        val lifecycle = LifecycleRegistry()
        return ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            history = history,
            completionClient = completionClient
        )
    }

    @Test
    fun sendUserMessage_createsConversationAndPersistsMessages() = runTest(testDispatcher) {
        val component = createComponent()

        component.sendUserMessage("Hello OpenCore")
        advanceUntilIdle()

        val state = component.state.value
        assertNotNull(state.activeConversation)
        assertEquals(2, state.messages.size)
        assertEquals(ChatMessageRole.USER, state.messages.first().role)
        assertEquals("Hello OpenCore", state.messages.first().content)
        assertEquals(ChatMessageRole.ASSISTANT, state.messages.last().role)
        assertEquals("Echo: Hello OpenCore", state.messages.last().content)

        val stored = history.listConversations()
        assertEquals(1, stored.size)
        assertEquals(2, history.loadMessages(stored.first().id).size)
    }

    @Test
    fun sendUserMessage_blankIsIgnored() = runTest(testDispatcher) {
        val component = createComponent()
        component.sendUserMessage("   ")
        advanceUntilIdle()
        assertFalse(component.state.value.isThreadActive)
    }

    @Test
    fun openConversation_loadsPersistedMessages() = runTest(testDispatcher) {
        val conversation = SidePanelConversation(title = "Saved")
        history.saveConversation(conversation)
        history.appendMessage(
            conversation.id,
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.USER,
                content = "Earlier",
                createdAt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )

        val component = createComponent()
        component.openConversation(conversation)
        advanceUntilIdle()

        assertEquals(conversation.id, component.state.value.activeConversation?.id)
        assertEquals(1, component.state.value.messages.size)
        assertEquals("Earlier", component.state.value.messages.first().content)
    }

    @Test
    fun startNewConversation_clearsThread() = runTest(testDispatcher) {
        val component = createComponent()
        component.sendUserMessage("Hi")
        advanceUntilIdle()

        component.startNewConversation()

        assertNull(component.state.value.activeConversation)
        assertTrue(component.state.value.messages.isEmpty())
    }

    @Test
    fun onActiveConversationDeleted_clearsMatchingThread() = runTest(testDispatcher) {
        val component = createComponent()
        component.sendUserMessage("Hi")
        advanceUntilIdle()
        val id = component.state.value.activeConversation!!.id

        component.onActiveConversationDeleted(id)

        assertNull(component.state.value.activeConversation)
    }

    @Test
    fun onActiveConversationRenamed_updatesHeaderTitle() = runTest(testDispatcher) {
        val component = createComponent()
        component.sendUserMessage("Hi")
        advanceUntilIdle()
        val id = component.state.value.activeConversation!!.id

        component.onActiveConversationRenamed(id, "Renamed chat")

        assertEquals("Renamed chat", component.state.value.headerTitle)
    }
}
