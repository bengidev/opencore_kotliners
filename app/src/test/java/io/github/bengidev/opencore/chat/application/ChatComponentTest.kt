package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.infrastructure.ChatStreamingClient
import io.github.bengidev.opencore.chat.infrastructure.EchoChatStreamingClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    private fun createComponent(): ChatComponent {
        val lifecycle = LifecycleRegistry()
        return ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            history = history,
            streamingClient = EchoChatStreamingClient()
        )
    }

    @Test
    fun sendUserMessage_createsConversationWithThinkingAndReply() = runTest(testDispatcher) {
        val component = createComponent()

        component.sendUserMessage("Hello OpenCore")
        advanceUntilIdle()

        val state = component.state.value
        assertNotNull(state.activeConversation)
        assertEquals(3, state.messages.size)
        assertEquals(ChatMessageRole.USER, state.messages[0].role)
        assertEquals(SidePanelMessageKind.THINKING, state.messages[1].kind)
        assertTrue(state.messages[1].isComplete)
        assertEquals(ChatMessageRole.ASSISTANT, state.messages[2].role)
        assertEquals("Echo: Hello OpenCore", state.messages[2].content)

        val stored = history.listConversations()
        assertEquals(1, stored.size)
        assertEquals(3, history.loadMessages(stored.first().id).size)
    }

    @Test
    fun sendUserMessage_blankIsIgnored() = runTest(testDispatcher) {
        val component = createComponent()
        component.sendUserMessage("   ")
        advanceUntilIdle()
        assertFalse(component.state.value.isThreadActive)
    }

    @Test
    fun sendUserMessage_ignoredWhileThreadIsLoading() = runTest(testDispatcher) {
        val conversation = SidePanelConversation(title = "Saved")
        history.saveConversation(conversation)
        val delayedHistory = DelayedHistoryRepository(history, loadDelayMs = 1_000)
        val component = ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
            history = delayedHistory,
            streamingClient = EchoChatStreamingClient()
        )

        component.openConversation(conversation)
        assertTrue(component.state.value.isLoadingMessages)

        component.sendUserMessage("Should not send")
        advanceUntilIdle()

        assertTrue(component.state.value.messages.isEmpty())
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
        assertFalse(component.state.value.isLoadingMessages)
    }

    @Test
    fun openConversation_staleLoadDoesNotOverwriteNewerSelection() = runTest(testDispatcher) {
        val first = SidePanelConversation(title = "First")
        val second = SidePanelConversation(title = "Second")
        history.saveConversation(first)
        history.saveConversation(second)
        history.appendMessage(
            first.id,
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.USER,
                content = "First thread",
                createdAt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )
        history.appendMessage(
            second.id,
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.USER,
                content = "Second thread",
                createdAt = Instant.parse("2024-01-01T00:01:00Z")
            )
        )

        val delayedHistory = DelayedHistoryRepository(history, loadDelayMs = 1_000)
        val component = ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
            history = delayedHistory,
            streamingClient = EchoChatStreamingClient()
        )

        component.openConversation(first)
        component.openConversation(second)
        advanceUntilIdle()

        assertEquals(second.id, component.state.value.activeConversation?.id)
        assertEquals("Second thread", component.state.value.messages.single().content)
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
    fun retry_usesCurrentProviderSortByNotPreviousSend() = runTest(testDispatcher) {
        val recordingClient = RecordingChatStreamingClient()
        val component = ChatComponent(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
            history = history,
            streamingClient = recordingClient
        )

        component.sendUserMessage("Hello", providerSortBy = "throughput")
        advanceUntilIdle()
        assertEquals("throughput", recordingClient.lastProviderSortBy)

        component.retry(providerSortBy = null)
        advanceUntilIdle()
        assertEquals(null, recordingClient.lastProviderSortBy)
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

private class RecordingChatStreamingClient : ChatStreamingClient {
    var lastProviderSortBy: String? = null

    override fun stream(
        messages: List<SidePanelMessage>,
        providerSortBy: String?,
        reasoningEffort: String?
    ): Flow<ChatStreamingEvent> {
        lastProviderSortBy = providerSortBy
        return flow {
            emit(ChatStreamingEvent.TextDelta("ok"))
            emit(ChatStreamingEvent.Done)
        }
    }
}

private class DelayedHistoryRepository(
    private val delegate: InMemorySidePanelHistoryRepository,
    private val loadDelayMs: Long
) : PersistenceConversationHistoryStoring {
    override suspend fun listConversations() = delegate.listConversations()
    override suspend fun saveConversation(conversation: SidePanelConversation) =
        delegate.saveConversation(conversation)
    override suspend fun appendMessage(conversationId: UUID, message: SidePanelMessage) =
        delegate.appendMessage(conversationId, message)
    override suspend fun loadMessages(conversationId: UUID): List<SidePanelMessage> {
        delay(loadDelayMs)
        return delegate.loadMessages(conversationId)
    }
    override suspend fun deleteConversation(conversationId: UUID) =
        delegate.deleteConversation(conversationId)
    override suspend fun setPinned(conversationId: UUID, isPinned: Boolean) =
        delegate.setPinned(conversationId, isPinned)
    override suspend fun renameConversation(conversationId: UUID, title: String) =
        delegate.renameConversation(conversationId, title)
    override suspend fun setGroup(conversationId: UUID, groupName: String?) =
        delegate.setGroup(conversationId, groupName)
    override suspend fun listGroups(): List<String> = delegate.listGroups()
}
