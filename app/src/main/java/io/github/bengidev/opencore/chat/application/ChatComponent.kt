package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatCompletionClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

internal class ChatComponent(
    componentContext: ComponentContext,
    private val history: SidePanelHistoryRepository,
    private val completionClient: ChatCompletionClient
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(ChatState())
    val state: Value<ChatState> = _state

    var onActiveConversationChanged: ((UUID?) -> Unit)? = null
    var onHistoryChanged: (() -> Unit)? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    fun dispatch(intent: ChatIntent) {
        _state.update { current -> ChatReducer.reduce(current, intent) }
    }

    fun startNewConversation() {
        dispatch(ChatIntent.NewConversation)
        onActiveConversationChanged?.invoke(null)
    }

    fun openConversation(conversation: SidePanelConversation) {
        dispatch(ChatIntent.ConversationOpened(conversation))
        onActiveConversationChanged?.invoke(conversation.id)
        scope.launch {
            val messages = history.loadMessages(conversation.id)
            dispatch(ChatIntent.MessagesLoaded(messages))
        }
    }

    fun onActiveConversationRenamed(id: UUID, title: String) {
        dispatch(ChatIntent.ActiveConversationRenamed(id, title))
    }

    fun onActiveConversationDeleted(id: UUID) {
        val wasActive = _state.value.activeConversation?.id == id
        dispatch(ChatIntent.ActiveConversationDeleted(id))
        if (wasActive) {
            onActiveConversationChanged?.invoke(null)
        }
    }

    fun sendUserMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        scope.launch {
            val conversation = ensureActiveConversation(text)
            val userMessage = SidePanelMessage(
                id = UUID.randomUUID(),
                role = ChatMessageRole.USER,
                content = text,
                createdAt = Instant.now()
            )
            history.appendMessage(conversation.id, userMessage)
            dispatch(ChatIntent.UserMessageAppended(userMessage))

            dispatch(ChatIntent.SendStarted)
            val assistantMessage = completionClient.complete(_state.value.messages)
            history.appendMessage(conversation.id, assistantMessage)
            dispatch(ChatIntent.AssistantMessageAppended(assistantMessage))
            dispatch(ChatIntent.SendFinished)
            onHistoryChanged?.invoke()
        }
    }

    private suspend fun ensureActiveConversation(firstMessage: String): SidePanelConversation {
        val existing = _state.value.activeConversation
        if (existing != null) return existing

        val conversation = SidePanelConversation(
            title = firstMessage.take(80),
            updatedAt = Instant.now()
        )
        history.saveConversation(conversation)
        dispatch(ChatIntent.ConversationOpened(conversation))
        onActiveConversationChanged?.invoke(conversation.id)
        onHistoryChanged?.invoke()
        return conversation
    }
}
