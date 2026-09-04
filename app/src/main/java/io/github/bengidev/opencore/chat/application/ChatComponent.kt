package io.github.bengidev.opencore.chat.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.chat.infrastructure.ChatTextMessageDetailCodec
import io.github.bengidev.opencore.chat.utilities.ChatModelInputBuilder
import io.github.bengidev.opencore.chat.utilities.ChatMultimodalWireLogic
import io.github.bengidev.opencore.chat.infrastructure.ChatStreamingClient
import io.github.bengidev.opencore.sidepanel.domain.ConversationTitlePolicy
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.atoms.domain.toAtom
import io.github.bengidev.opencore.chat.utilities.ChatContextOverflowDetector
import io.github.bengidev.opencore.chat.utilities.SettingsContextCompactionClient
import io.github.bengidev.opencore.chat.utilities.SettingsContextCompactionException
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryError
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryStoring
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
    private val history: PersistenceAtomHistoryStoring,
    private val streamingClient: ChatStreamingClient,
    private val contextCompaction: SettingsContextCompactionClient = SettingsContextCompactionClient.disabled,
    private val contextLengthProvider: suspend () -> Int? = { null },
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(ChatState())
    val state: Value<ChatState> = _state
    private var streamJob: Job? = null
    private var streamingFlushJob: Job? = null
    private val streamingCoalescer = ChatStreamingCoalescer()
    private var activeStreamId = 0
    private var loadGeneration = 0
    private var lastProviderSortBy: String? = null
    private var lastReasoningEffort: String? = null
    private var didOverflowRetry = false

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
            val messages = history.loadProjectedChatMessages(conversation.id)
            if (generation != loadGeneration) return@launch
            dispatch(ChatIntent.MessagesLoaded(conversation.id, messages))
        }
    }

    fun compactContextManually() {
        val conversation = _state.value.activeConversation ?: return
        if (_state.value.isSending || _state.value.isCompacting || !_state.value.hasMessages) return
        scope.launch {
            dispatch(ChatIntent.CompactingStarted)
            try {
                val contextLength = contextLengthProvider()
                if (contextLength == null || contextLength <= 0) {
                    dispatch(ChatIntent.CompactionFailed("Select a model before compacting context."))
                    return@launch
                }
                val sessionEntries = history.loadSessionEntries(conversation.id)
                val leafEntryId = history.loadLeafEntryId(conversation.id)
                val outcome = contextCompaction.compactManually(
                    messages = _state.value.messages,
                    sessionEntries = sessionEntries,
                    leafEntryId = leafEntryId,
                    contextLength = contextLength,
                )
                if (outcome.checkpoint == null && outcome.projectedMessages == _state.value.messages) {
                    dispatch(ChatIntent.CompactionFailed("Not enough conversation history to compact yet."))
                    return@launch
                }
                if (outcome.checkpoint != null) {
                    history.appendCompaction(conversation.id, outcome.checkpoint)
                }
                dispatch(ChatIntent.MessagesLoaded(conversation.id, outcome.projectedMessages))
                dispatch(ChatIntent.CompactingFinished)
                onHistoryChanged?.invoke()
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Could not compact conversation context."
                dispatch(ChatIntent.CompactionFailed(message))
            }
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

    fun addDraftAttachment(attachment: ChatMessageAttachment) {
        dispatch(ChatIntent.DraftAttachmentAdded(attachment))
    }

    fun removeDraftAttachment(id: UUID) {
        dispatch(ChatIntent.DraftAttachmentRemoved(id))
    }

    fun clearDraftAttachments() {
        dispatch(ChatIntent.DraftAttachmentsCleared)
    }

    fun sendUserMessage(rawText: String, providerSortBy: String? = null, reasoningEffort: String? = null) {
        val visibleText = rawText.trim()
        val attachments = _state.value.draftAttachments
        val current = _state.value
        if ((visibleText.isEmpty() && attachments.isEmpty()) || current.isSending || current.isLoadingMessages) return

        val modelContent = ChatModelInputBuilder.modelContent(visibleText, attachments)
        val preparedAttachments = try {
            ChatMultimodalWireLogic.prepareAttachmentsForSend(
                attachments = attachments,
                modelText = modelContent,
            )
        } catch (error: Exception) {
            dispatch(ChatIntent.SendPreparationFailed(error.message ?: "Could not prepare attachments for sending."))
            return
        }

        val detailJson = ChatTextMessageDetailCodec.encode(
            attachments = preparedAttachments,
            modelContent = modelContent.takeIf { it.isNotBlank() },
        )
        val userMessage = SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = visibleText,
            createdAt = Instant.now(),
            detailJson = detailJson,
        )
        dispatch(ChatIntent.DraftAttachmentsCommitted)
        val activeConversation = current.activeConversation
        if (activeConversation != null) {
            appendUserTurn(
                conversationId = activeConversation.id,
                userMessageText = visibleText,
                attachments = attachments,
                userMessage = userMessage,
                providerSortBy = providerSortBy,
                reasoningEffort = reasoningEffort,
            )
            return
        }

        beginNewConversationTurn(
            userMessageText = visibleText,
            attachments = attachments,
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
        reasoningEffort: String? = null,
        overflowRetry: Boolean = false,
    ) {
        cancelStream()
        resetStreamingBuffers()
        val streamId = ++activeStreamId
        if (!overflowRetry) {
            didOverflowRetry = false
        }
        lastProviderSortBy = providerSortBy
        lastReasoningEffort = reasoningEffort

        val wireMessages = try {
            prepareMessagesForWire(conversationId, overflowRetry)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportStreamFailure(e, conversationId)
            return
        }

        dispatch(ChatIntent.StreamingTurnStarted)
        var scheduledOverflowRetry = false

        streamJob = scope.launch {
            try {
                streamingClient.stream(wireMessages, providerSortBy, reasoningEffort).collect { event ->
                    if (streamId != activeStreamId) return@collect
                    handleStreamingEvent(
                        event = event,
                        conversationId = conversationId,
                        onOverflowRetryScheduled = { scheduledOverflowRetry = true },
                    )
                }
                if (streamId == activeStreamId &&
                    _state.value.streamingStatus == ChatStreamingStatus.Running &&
                    !scheduledOverflowRetry
                ) {
                    handleStreamingEvent(ChatStreamingEvent.Done, conversationId)
                }
            } catch (_: CancellationException) {
                // Turn cancelled (new message, new chat) — do not finalize.
            } catch (e: Exception) {
                if (streamId == activeStreamId) {
                    handleStreamingEvent(
                        ChatStreamingEvent.Error(ChatStreamError(formatStreamFailure(e))),
                        conversationId,
                    )
                }
            }
        }
        streamJob?.join()
        onHistoryChanged?.invoke()
    }

    private suspend fun prepareMessagesForWire(
        conversationId: UUID,
        overflowRetry: Boolean,
    ): List<SidePanelMessage> {
        val contextLength = contextLengthProvider() ?: 0
        val sessionEntries = history.loadSessionEntries(conversationId)
        val leafEntryId = history.loadLeafEntryId(conversationId)
        val currentMessages = _state.value.messages.filter { message ->
            message.isComplete || message.role != ChatMessageRole.ASSISTANT
        }

        val outcome = if (overflowRetry) {
            contextCompaction.compactForOverflow(
                messages = currentMessages,
                sessionEntries = sessionEntries,
                leafEntryId = leafEntryId,
                contextLength = contextLength,
            )
        } else {
            contextCompaction.compactIfNeeded(
                messages = currentMessages,
                sessionEntries = sessionEntries,
                leafEntryId = leafEntryId,
                contextLength = contextLength,
            )
        }

        if (outcome.checkpoint != null) {
            history.appendCompaction(conversationId, outcome.checkpoint)
        }
        if (outcome.projectedMessages != currentMessages) {
            dispatch(ChatIntent.MessagesLoaded(conversationId, outcome.projectedMessages))
        }
        return outcome.projectedMessages
    }

    private suspend fun handleStreamingEvent(
        event: ChatStreamingEvent,
        conversationId: UUID,
        onOverflowRetryScheduled: () -> Unit = {},
    ) {
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
                if (
                    !didOverflowRetry &&
                    ChatContextOverflowDetector.isContextOverflow(event.error.message)
                ) {
                    didOverflowRetry = true
                    onOverflowRetryScheduled()
                    scope.launch {
                        startStream(
                            conversationId = conversationId,
                            providerSortBy = lastProviderSortBy,
                            reasoningEffort = lastReasoningEffort,
                            overflowRetry = true,
                        )
                    }
                    return
                }
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
            history.appendChatMessage(conversationId, message)
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
        attachments: List<ChatMessageAttachment>,
        userMessage: SidePanelMessage,
        providerSortBy: String?,
        reasoningEffort: String?,
    ) {
        dispatch(ChatIntent.UserMessageAppended(userMessage))
        syncConversationTitle(conversationId, userMessageText, attachments)
        launchPersistAndStream(conversationId, userMessage, providerSortBy, reasoningEffort)
    }

    private fun beginNewConversationTurn(
        userMessageText: String,
        attachments: List<ChatMessageAttachment>,
        userMessage: SidePanelMessage,
        providerSortBy: String?,
        reasoningEffort: String?,
    ) {
        val conversation = SidePanelConversation(
            title = ConversationTitlePolicy.fromUserMessage(userMessageText, attachments).ifBlank { "New chat" },
            updatedAt = Instant.now(),
        )
        dispatch(ChatIntent.ConversationOpened(conversation, loadMessages = false))
        dispatch(ChatIntent.UserMessageAppended(userMessage))
        onActiveConversationChanged?.invoke(conversation.id)
        scope.launch {
            try {
                history.saveAtom(conversation.toAtom())
                onHistoryChanged?.invoke()
                history.appendChatMessage(conversation.id, userMessage)
                startStream(conversation.id, providerSortBy, reasoningEffort)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportStreamFailure(e, conversation.id)
            }
        }
    }

    private fun launchPersistAndStream(
        conversationId: UUID,
        userMessage: SidePanelMessage,
        providerSortBy: String?,
        reasoningEffort: String?,
    ) {
        scope.launch {
            try {
                history.appendChatMessage(conversationId, userMessage)
                startStream(conversationId, providerSortBy, reasoningEffort)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportStreamFailure(e, conversationId)
            }
        }
    }

    private suspend fun reportStreamFailure(error: Exception, conversationId: UUID) {
        val message = formatStreamFailure(error)
        if (_state.value.isSending) {
            handleStreamingEvent(ChatStreamingEvent.Error(ChatStreamError(message)), conversationId)
            resetStreamingBuffers()
        } else {
            dispatch(ChatIntent.SendPreparationFailed(message))
        }
    }

    private fun formatStreamFailure(error: Exception): String = when (error) {
        is SettingsContextCompactionException ->
            error.message ?: "Could not compact conversation context."
        is PersistenceAtomHistoryError ->
            "Could not save this conversation yet. Try again."
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Could not send message."
    }

    private fun syncConversationTitle(
        conversationId: UUID,
        userMessageText: String,
        attachments: List<ChatMessageAttachment>,
    ) {
        val newTitle = ConversationTitlePolicy.fromUserMessage(userMessageText, attachments)
        if (newTitle.isEmpty()) return
        val currentTitle = _state.value.activeConversation?.title ?: return
        if (currentTitle == newTitle) return
        dispatch(ChatIntent.ActiveConversationRenamed(conversationId, newTitle))
        onConversationTitleChanged?.invoke(conversationId, newTitle)
        scope.launch {
            history.renameAtom(conversationId, newTitle)
        }
    }
}
