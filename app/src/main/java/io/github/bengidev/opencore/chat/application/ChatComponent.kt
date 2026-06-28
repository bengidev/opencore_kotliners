package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.infrastructure.ChatStreamingClient
import io.github.bengidev.opencore.sidepanel.domain.ConversationTitlePolicy
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring
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
    private val history: PersistenceConversationHistoryStoring,
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
    var onConversationTitleChanged: ((UUID, String) -> Unit)? = null

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

    fun sendUserMessage(rawText: String, providerSortBy: String? = null, reasoningEffort: String? = null) {
        val text = rawText.trim()
        val current = _state.value
        if (text.isEmpty() || current.isSending || current.isLoadingMessages) return

        val userMessage = SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = text,
            createdAt = Instant.now()
        )
        val activeConversation = current.activeConversation
        if (activeConversation != null) {
            appendUserTurn(
                conversationId = activeConversation.id,
                userMessageText = text,
                userMessage = userMessage,
                providerSortBy = providerSortBy,
                reasoningEffort = reasoningEffort,
            )
            return
        }

        beginNewConversationTurn(
            userMessageText = text,
            userMessage = userMessage,
            providerSortBy = providerSortBy,
            reasoningEffort = reasoningEffort,
        )
    }

    fun retry(providerSortBy: String? = null, reasoningEffort: String? = null) {
        val conversation = _state.value.activeConversation ?: return
        if (_state.value.isSending) return
        scope.launch {
            startStream(conversation.id, providerSortBy, reasoningEffort)
        }
    }

    private suspend fun startStream(
        conversationId: UUID,
        providerSortBy: String? = null,
        reasoningEffort: String? = null
    ) {
        cancelStream()
        resetStreamingBuffers()
        val streamId = ++activeStreamId
        dispatch(ChatIntent.StreamingTurnStarted)

        val messagesForWire = _state.value.messages.filter { message ->
            message.isComplete || message.role != ChatMessageRole.ASSISTANT
        }

        streamJob = scope.launch {
            try {
                streamingClient.stream(messagesForWire, providerSortBy, reasoningEffort).collect { event ->
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
            is ChatStreamingEvent.TextDelta,
            is ChatStreamingEvent.OutputStreamDelta -> {
                if (streamingCoalescer.accumulate(event)) {
                    scheduleStreamingFlush()
                }
            }
            is ChatStreamingEvent.OutputStreamBegan -> {
                val mergeResult = ChatStreamingMerger.merge(
                    state = _state.value.toStreamingState(),
                    event = event,
                    makeId = { UUID.randomUUID() },
                    now = Instant.now()
                )
                commitStreamingMerge(mergeResult, conversationId, bumpStreamingRevision = true)
                flushStreamingNow()
            }
            is ChatStreamingEvent.OutputStreamEnded -> {
                flushStreamingNow()
                val mergeResult = ChatStreamingMerger.merge(
                    state = _state.value.toStreamingState(),
                    event = event,
                    makeId = { UUID.randomUUID() },
                    now = Instant.now()
                )
                commitStreamingMerge(mergeResult, conversationId, bumpStreamingRevision = true)
            }
            ChatStreamingEvent.Done -> {
                flushStreamingNow()
                val mergeResult = ChatStreamingMerger.merge(
                    state = _state.value.toStreamingState(),
                    event = event,
                    makeId = { UUID.randomUUID() },
                    now = Instant.now()
                )
                commitStreamingMerge(mergeResult, conversationId, bumpStreamingRevision = true)
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
                commitStreamingMerge(mergeResult, conversationId, bumpStreamingRevision = true)
                resetStreamingBuffers()
            }
        }
    }

    private fun applyStreamingMerge(
        mergeResult: ChatStreamingMergeResult,
        bumpStreamingRevision: Boolean,
    ) {
        dispatch(ChatIntent.StreamingMerged(mergeResult, bumpStreamingRevision = bumpStreamingRevision))
    }

    private suspend fun persistFinalizedMessages(
        conversationId: UUID,
        mergeResult: ChatStreamingMergeResult,
    ) {
        mergeResult.finalizedMessages.forEach { message ->
            history.appendMessage(conversationId, message)
        }
    }

    private suspend fun commitStreamingMerge(
        mergeResult: ChatStreamingMergeResult,
        conversationId: UUID,
        bumpStreamingRevision: Boolean,
    ) {
        applyStreamingMerge(mergeResult, bumpStreamingRevision)
        persistFinalizedMessages(conversationId, mergeResult)
    }

    private fun scheduleStreamingFlush() {
        if (streamingFlushJob != null) return
        val delayMs = ChatStreamingCoalescingPolicy.flushDelayMs(streamingCoalescer.pendingByteCount)
        streamingFlushJob = scope.launch {
            delay(delayMs)
            streamingFlushJob = null
            applyPendingStreamingUI()
        }
    }

    private fun flushStreamingNow() {
        cancelStreamingFlush()
        applyPendingStreamingUI()
    }

    private fun applyPendingStreamingUI() {
        val outputDelta = streamingCoalescer.consumeOutputStreamDelta()
        val mergeResult = ChatStreamingMerger.applyPendingPartial(
            state = _state.value.toStreamingState(),
            partialThinking = streamingCoalescer.accumulatedThinking,
            partialText = streamingCoalescer.accumulatedText,
            partialOutputStreamDelta = outputDelta,
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
        flushStreamingNow()
        val conversationId = _state.value.activeConversation?.id
        if (_state.value.streamingOutputStreamId != null && conversationId != null) {
            val mergeResult = ChatStreamingMerger.merge(
                state = _state.value.toStreamingState(),
                event = ChatStreamingEvent.OutputStreamEnded(
                    status = ChatOutputStreamStatus.FAILED,
                    exitCode = null,
                    durationMs = null,
                ),
                makeId = { UUID.randomUUID() },
                now = Instant.now()
            )
            applyStreamingMerge(mergeResult, bumpStreamingRevision = false)
            scope.launch {
                persistFinalizedMessages(conversationId, mergeResult)
            }
        }
        cancelStreamingFlush()
        streamJob?.cancel()
        streamJob = null
    }

    private fun appendUserTurn(
        conversationId: UUID,
        userMessageText: String,
        userMessage: SidePanelMessage,
        providerSortBy: String?,
        reasoningEffort: String?,
    ) {
        dispatch(ChatIntent.UserMessageAppended(userMessage))
        syncConversationTitle(conversationId, userMessageText)
        launchPersistAndStream(conversationId, userMessage, providerSortBy, reasoningEffort)
    }

    private fun beginNewConversationTurn(
        userMessageText: String,
        userMessage: SidePanelMessage,
        providerSortBy: String?,
        reasoningEffort: String?,
    ) {
        val conversation = SidePanelConversation(
            title = ConversationTitlePolicy.fromUserMessage(userMessageText),
            updatedAt = Instant.now(),
        )
        dispatch(ChatIntent.ConversationOpened(conversation, loadMessages = false))
        dispatch(ChatIntent.UserMessageAppended(userMessage))
        onActiveConversationChanged?.invoke(conversation.id)
        scope.launch {
            history.saveConversation(conversation)
            onHistoryChanged?.invoke()
        }
        launchPersistAndStream(conversation.id, userMessage, providerSortBy, reasoningEffort)
    }

    private fun launchPersistAndStream(
        conversationId: UUID,
        userMessage: SidePanelMessage,
        providerSortBy: String?,
        reasoningEffort: String?,
    ) {
        scope.launch {
            startStream(conversationId, providerSortBy, reasoningEffort)
        }
        scope.launch {
            history.appendMessage(conversationId, userMessage)
        }
    }

    private fun syncConversationTitle(conversationId: UUID, userMessageText: String) {
        val newTitle = ConversationTitlePolicy.fromUserMessage(userMessageText)
        if (newTitle.isEmpty()) return
        val currentTitle = _state.value.activeConversation?.title ?: return
        if (currentTitle == newTitle) return
        dispatch(ChatIntent.ActiveConversationRenamed(conversationId, newTitle))
        onConversationTitleChanged?.invoke(conversationId, newTitle)
        scope.launch {
            history.renameConversation(conversationId, newTitle)
        }
    }
}
