package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.infrastructure.ChatStreamingClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

internal class ChatComponent(
    componentContext: ComponentContext,
    private val history: SidePanelHistoryRepository,
    private val streamingClient: ChatStreamingClient
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(ChatState())
    val state: Value<ChatState> = _state
    private var streamJob: Job? = null

    var onActiveConversationChanged: ((UUID?) -> Unit)? = null
    var onHistoryChanged: (() -> Unit)? = null

    init {
        lifecycle.doOnDestroy {
            streamJob?.cancel()
            scope.cancel()
        }
    }

    fun dispatch(intent: ChatIntent) {
        _state.update { current -> ChatReducer.reduce(current, intent) }
    }

    fun startNewConversation() {
        cancelStream()
        dispatch(ChatIntent.NewConversation)
        onActiveConversationChanged?.invoke(null)
    }

    fun openConversation(conversation: SidePanelConversation) {
        cancelStream()
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

    fun dismissError() {
        dispatch(ChatIntent.StreamingErrorDismissed)
    }

    fun sendUserMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _state.value.isSending) return

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
            startStream(conversation.id)
        }
    }

    fun retry() {
        val conversation = _state.value.activeConversation ?: return
        if (_state.value.isSending) return
        scope.launch {
            startStream(conversation.id)
        }
    }

    private suspend fun startStream(conversationId: UUID) {
        cancelStream()
        dispatch(ChatIntent.StreamingTurnStarted)

        streamJob = scope.launch {
            try {
                streamingClient.stream(_state.value.messages).collect { event ->
                    handleStreamingEvent(event, conversationId)
                }
            } finally {
                if (_state.value.streamingStatus == ChatStreamingStatus.Running) {
                    handleStreamingEvent(ChatStreamingEvent.Done, conversationId)
                }
            }
        }
        streamJob?.join()
        onHistoryChanged?.invoke()
    }

    private fun handleStreamingEvent(event: ChatStreamingEvent, conversationId: UUID) {
        val mergeResult = ChatStreamingMerger.merge(
            state = _state.value.toStreamingState(),
            event = event,
            makeId = { UUID.randomUUID() },
            now = Instant.now()
        )
        dispatch(ChatIntent.StreamingMerged(mergeResult))

        mergeResult.finalizedMessages.forEach { message ->
            scope.launch {
                history.appendMessage(conversationId, message)
            }
        }
    }

    private fun cancelStream() {
        streamJob?.cancel()
        streamJob = null
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
