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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private var streamingFlushJob: Job? = null
    private val streamingCoalescer = ChatStreamingCoalescer()
    private var activeStreamId = 0
    private var loadGeneration = 0

    var onActiveConversationChanged: ((UUID?) -> Unit)? = null
    var onHistoryChanged: (() -> Unit)? = null

    init {
        lifecycle.doOnDestroy {
            cancelStreamingFlush()
            streamJob?.cancel()
            scope.cancel()
        }
    }

    fun dispatch(intent: ChatIntent) {
        _state.update { current -> ChatReducer.reduce(current, intent) }
    }

    fun startNewConversation() {
        cancelStream()
        ++loadGeneration
        dispatch(ChatIntent.NewConversation)
        onActiveConversationChanged?.invoke(null)
    }

    fun openConversation(conversation: SidePanelConversation) {
        cancelStream()
        val generation = ++loadGeneration
        dispatch(ChatIntent.ConversationOpened(conversation))
        onActiveConversationChanged?.invoke(conversation.id)
        scope.launch {
            val messages = history.loadMessages(conversation.id)
            if (generation != loadGeneration) return@launch
            dispatch(ChatIntent.MessagesLoaded(conversation.id, messages))
        }
    }

    fun onActiveConversationRenamed(id: UUID, title: String) {
        dispatch(ChatIntent.ActiveConversationRenamed(id, title))
    }

    fun onActiveConversationDeleted(id: UUID) {
        val wasActive = _state.value.activeConversation?.id == id
        if (wasActive) {
            ++loadGeneration
        }
        dispatch(ChatIntent.ActiveConversationDeleted(id))
        if (wasActive) {
            onActiveConversationChanged?.invoke(null)
        }
    }

    fun dismissError() {
        dispatch(ChatIntent.StreamingErrorDismissed)
    }

    fun sendUserMessage(rawText: String, providerSortBy: String? = null) {
        val text = rawText.trim()
        val current = _state.value
        if (text.isEmpty() || current.isSending || current.isLoadingMessages) return

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
            startStream(conversation.id, providerSortBy)
        }
    }

    fun retry(providerSortBy: String? = null) {
        val conversation = _state.value.activeConversation ?: return
        if (_state.value.isSending) return
        scope.launch {
            startStream(conversation.id, providerSortBy)
        }
    }

    private suspend fun startStream(conversationId: UUID, providerSortBy: String? = null) {
        cancelStream()
        resetStreamingBuffers()
        val streamId = ++activeStreamId
        dispatch(ChatIntent.StreamingTurnStarted)

        val messagesForWire = _state.value.messages.filter { message ->
            message.isComplete || message.role != ChatMessageRole.ASSISTANT
        }

        streamJob = scope.launch {
            try {
                streamingClient.stream(messagesForWire, providerSortBy).collect { event ->
                    if (streamId != activeStreamId) return@collect
                    handleStreamingEvent(event, conversationId)
                }
                if (streamId == activeStreamId &&
                    _state.value.streamingStatus == ChatStreamingStatus.Running
                ) {
                    handleStreamingEvent(ChatStreamingEvent.Done, conversationId)
                }
            } catch (_: CancellationException) {
                // Turn cancelled (new message, new chat) — do not finalize.
            }
        }
        streamJob?.join()
        onHistoryChanged?.invoke()
    }

    private suspend fun handleStreamingEvent(event: ChatStreamingEvent, conversationId: UUID) {
        when (event) {
            is ChatStreamingEvent.ThinkingDelta,
            is ChatStreamingEvent.TextDelta -> {
                if (streamingCoalescer.accumulate(event)) {
                    scheduleStreamingFlush(conversationId)
                }
            }
            ChatStreamingEvent.Done -> {
                flushStreamingNow()
                val mergeResult = ChatStreamingMerger.merge(
                    state = _state.value.toStreamingState(),
                    event = event,
                    makeId = { UUID.randomUUID() },
                    now = Instant.now()
                )
                dispatch(ChatIntent.StreamingMerged(mergeResult, bumpStreamingRevision = true))
                mergeResult.finalizedMessages.forEach { message ->
                    history.appendMessage(conversationId, message)
                }
                resetStreamingBuffers()
            }
            is ChatStreamingEvent.Error -> {
                flushStreamingNow()
                val mergeResult = ChatStreamingMerger.merge(
                    state = _state.value.toStreamingState(),
                    event = event,
                    makeId = { UUID.randomUUID() },
                    now = Instant.now()
                )
                dispatch(ChatIntent.StreamingMerged(mergeResult, bumpStreamingRevision = true))
                resetStreamingBuffers()
            }
        }
    }

    private fun scheduleStreamingFlush(conversationId: UUID) {
        if (streamingFlushJob != null) return
        val delayMs = ChatStreamingCoalescingPolicy.flushDelayMs(streamingCoalescer.pendingByteCount)
        streamingFlushJob = scope.launch {
            delay(delayMs)
            streamingFlushJob = null
            applyPendingStreamingUI()
        }
    }

    private fun flushStreamingNow(conversationId: UUID) {
        cancelStreamingFlush()
        applyPendingStreamingUI()
    }

    private fun applyPendingStreamingUI() {
        val mergeResult = ChatStreamingMerger.applyPendingPartial(
            state = _state.value.toStreamingState(),
            partialThinking = streamingCoalescer.accumulatedThinking,
            partialText = streamingCoalescer.accumulatedText,
            makeId = { UUID.randomUUID() },
            now = Instant.now()
        )
        if (mergeResult.state == _state.value.toStreamingState()) return
        dispatch(ChatIntent.StreamingMerged(mergeResult, bumpStreamingRevision = true))
    }

    private fun resetStreamingBuffers() {
        cancelStreamingFlush()
        streamingCoalescer.reset()
    }

    private fun cancelStreamingFlush() {
        streamingFlushJob?.cancel()
        streamingFlushJob = null
    }

    private fun cancelStream() {
        cancelStreamingFlush()
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
        dispatch(ChatIntent.ConversationOpened(conversation, loadMessages = false))
        onActiveConversationChanged?.invoke(conversation.id)
        onHistoryChanged?.invoke()
        return conversation
    }
}
