package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.chat.utilities.ChatAssistantContentNormalizer
import java.time.Instant
import java.util.UUID

/** Snapshot of streaming merge inputs. Command-pattern state slice for `ChatStreamingMerger`. */
internal data class ChatStreamingState(
    val messages: List<SidePanelMessage>,
    val currentPartialText: String = "",
    val currentPartialThinking: String = "",
    val streamingThinkingId: UUID? = null,
    val streamingAnswerId: UUID? = null,
    val streamingOutputStreamId: UUID? = null,
    val streamingStatus: ChatStreamingStatus = ChatStreamingStatus.Idle,
    val streamErrorMessage: String? = null
)

internal data class ChatStreamingMergeResult(
    val state: ChatStreamingState,
    val finalizedMessages: List<SidePanelMessage> = emptyList()
)

/** Pure merge logic for SSE stream events. */
internal object ChatStreamingMerger {
    internal const val EMPTY_RESPONSE_MESSAGE = "No response received from the provider."

    fun applyPendingPartial(
        state: ChatStreamingState,
        partialThinking: String,
        partialText: String,
        partialOutputStreamDelta: String,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        var current = state
        var didChange = false

        val trimmedThinking = partialThinking.trim()
        if (trimmedThinking.isNotEmpty()) {
            current = upsertThinkingRow(current, partialThinking, makeId, now).state
            didChange = true
        }

        if (partialText.isNotEmpty()) {
            current = upsertAnswerRow(current, partialText, makeId, now).state
            didChange = true
        }

        if (partialOutputStreamDelta.isNotEmpty()) {
            current = appendOutputStreamDelta(current, partialOutputStreamDelta).state
            didChange = true
        }

        if (!didChange) {
            return ChatStreamingMergeResult(state = current)
        }

        return ChatStreamingMergeResult(
            state = current.copy(
                currentPartialText = partialText,
                currentPartialThinking = partialThinking,
                streamingStatus = ChatStreamingStatus.Running
            )
        )
    }

    fun merge(
        state: ChatStreamingState,
        event: ChatStreamingEvent,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult = when (event) {
        is ChatStreamingEvent.ThinkingDelta -> mergeThinkingDelta(state, event.text, makeId, now)
        is ChatStreamingEvent.TextDelta -> mergeTextDelta(state, event.text, makeId, now)
        is ChatStreamingEvent.OutputStreamBegan -> beginOutputStream(state, event.command, event.cwd, makeId, now)
        is ChatStreamingEvent.OutputStreamEnded -> finalizeActiveOutputStream(
            state,
            event.status,
            event.exitCode,
            event.durationMs,
        )
        ChatStreamingEvent.Done -> mergeDone(state)
        is ChatStreamingEvent.Error -> mergeError(state, event.error.message)
        is ChatStreamingEvent.OutputStreamDelta -> ChatStreamingMergeResult(state = state)
    }

    private fun mergeThinkingDelta(
        state: ChatStreamingState,
        delta: String,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        val partialThinking = state.currentPartialThinking + delta
        if (partialThinking.trim().isEmpty()) {
            return ChatStreamingMergeResult(
                state.copy(
                    currentPartialThinking = partialThinking,
                    streamingStatus = ChatStreamingStatus.Running
                )
            )
        }
        return upsertThinkingRow(state, partialThinking, makeId, now)
    }

    private fun mergeTextDelta(
        state: ChatStreamingState,
        delta: String,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        val partialText = state.currentPartialText + delta
        return upsertAnswerRow(state, partialText, makeId, now)
    }

    private fun beginOutputStream(
        state: ChatStreamingState,
        command: String,
        cwd: String?,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return ChatStreamingMergeResult(state = state)

        var current = state
        var finalized = emptyList<SidePanelMessage>()
        if (current.streamingOutputStreamId != null) {
            // Provider began a new stream without an explicit end for the previous one.
            val finalizeResult = finalizeActiveOutputStream(
                current,
                ChatOutputStreamStatus.FAILED,
                exitCode = null,
                durationMs = null,
            )
            current = finalizeResult.state
            finalized = finalizeResult.finalizedMessages
        }

        val newId = makeId()
        val detail = ChatOutputStreamDetail(status = ChatOutputStreamStatus.RUNNING, cwd = cwd)
        val message = SidePanelMessage(
            id = newId,
            role = ChatMessageRole.SYSTEM,
            content = trimmed,
            createdAt = now,
            kind = SidePanelMessageKind.OUTPUT_STREAM,
            isComplete = false,
            detailJson = ChatOutputStreamDetailCodec.encode(detail),
        )
        return ChatStreamingMergeResult(
            state = current.copy(
                messages = current.messages + message,
                streamingOutputStreamId = newId,
                streamingStatus = ChatStreamingStatus.Running,
            ),
            finalizedMessages = finalized,
        )
    }

    private fun appendOutputStreamDelta(
        state: ChatStreamingState,
        delta: String,
    ): ChatStreamingMergeResult {
        val outputStreamId = state.streamingOutputStreamId ?: return ChatStreamingMergeResult(state = state)
        val messages = state.messages.map { message ->
            if (message.id != outputStreamId || message.kind != SidePanelMessageKind.OUTPUT_STREAM) {
                message
            } else {
                val detail = ChatOutputStreamDetailCodec.decode(message.detailJson, message.isComplete)
                    .appendOutput(delta)
                message.copy(
                    detailJson = ChatOutputStreamDetailCodec.encode(detail),
                    isComplete = false,
                )
            }
        }
        if (messages == state.messages) return ChatStreamingMergeResult(state = state)
        return ChatStreamingMergeResult(
            state = state.copy(
                messages = messages,
                streamingStatus = ChatStreamingStatus.Running,
            )
        )
    }

    private fun finalizeActiveOutputStream(
        state: ChatStreamingState,
        status: ChatOutputStreamStatus,
        exitCode: Int?,
        durationMs: Int?,
    ): ChatStreamingMergeResult {
        val outputStreamId = state.streamingOutputStreamId
        if (outputStreamId == null) return ChatStreamingMergeResult(state = state)

        val index = state.messages.indexOfFirst { it.id == outputStreamId }
        if (index < 0) {
            return ChatStreamingMergeResult(
                state = state.copy(streamingOutputStreamId = null)
            )
        }

        val existing = state.messages[index]
        if (existing.kind != SidePanelMessageKind.OUTPUT_STREAM) {
            return ChatStreamingMergeResult(
                state = state.copy(streamingOutputStreamId = null)
            )
        }

        val currentDetail = ChatOutputStreamDetailCodec.decode(existing.detailJson, existing.isComplete)
        val updatedDetail = currentDetail.copy(
            status = status,
            exitCode = exitCode ?: currentDetail.exitCode,
            durationMs = durationMs ?: currentDetail.durationMs,
        )
        val finalizedMessage = existing.copy(
            detailJson = ChatOutputStreamDetailCodec.encode(updatedDetail),
            isComplete = true,
        )
        val messages = state.messages.toMutableList().also { it[index] = finalizedMessage }

        return ChatStreamingMergeResult(
            state = state.copy(
                messages = messages,
                streamingOutputStreamId = null,
                streamingStatus = ChatStreamingStatus.Running,
            ),
            finalizedMessages = listOf(finalizedMessage),
        )
    }

    private fun upsertThinkingRow(
        state: ChatStreamingState,
        partialThinking: String,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        val thinkingId = resolveStreamingRowId(
            trackedId = state.streamingThinkingId,
            messages = state.messages,
            kind = SidePanelMessageKind.THINKING,
        )
        if (thinkingId != null) {
            val messages = state.messages.map { message ->
                if (message.id == thinkingId && message.kind == SidePanelMessageKind.THINKING) {
                    message.copy(content = partialThinking, isComplete = false)
                } else {
                    message
                }
            }
            if (messages.any { it.id == thinkingId && it.kind == SidePanelMessageKind.THINKING }) {
                return ChatStreamingMergeResult(
                    state.copy(
                        messages = messages,
                        currentPartialThinking = partialThinking,
                        streamingThinkingId = thinkingId,
                        streamingStatus = ChatStreamingStatus.Running
                    )
                )
            }
        }
        val newId = makeId()
        val thinkingMessage = SidePanelMessage(
            id = newId,
            role = ChatMessageRole.ASSISTANT,
            content = partialThinking,
            createdAt = now,
            kind = SidePanelMessageKind.THINKING,
            isComplete = false
        )
        return ChatStreamingMergeResult(
            state.copy(
                messages = state.messages + thinkingMessage,
                currentPartialThinking = partialThinking,
                streamingThinkingId = newId,
                streamingStatus = ChatStreamingStatus.Running
            )
        )
    }

    private fun upsertAnswerRow(
        state: ChatStreamingState,
        partialText: String,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        val answerId = resolveStreamingRowId(
            trackedId = state.streamingAnswerId,
            messages = state.messages,
            kind = SidePanelMessageKind.TEXT,
        )
        if (answerId != null) {
            val messages = state.messages.map { message ->
                if (message.id == answerId && message.kind == SidePanelMessageKind.TEXT) {
                    message.copy(content = partialText, isComplete = false)
                } else {
                    message
                }
            }
            if (messages.any { it.id == answerId && it.kind == SidePanelMessageKind.TEXT }) {
                return ChatStreamingMergeResult(
                    state.copy(
                        messages = messages,
                        currentPartialText = partialText,
                        streamingAnswerId = answerId,
                        streamingStatus = ChatStreamingStatus.Running
                    )
                )
            }
        }
        val newId = makeId()
        val answerMessage = SidePanelMessage(
            id = newId,
            role = ChatMessageRole.ASSISTANT,
            content = partialText,
            createdAt = now,
            kind = SidePanelMessageKind.TEXT,
            isComplete = false
        )
        return ChatStreamingMergeResult(
            state.copy(
                messages = state.messages + answerMessage,
                currentPartialText = partialText,
                streamingAnswerId = newId,
                streamingStatus = ChatStreamingStatus.Running
            )
        )
    }

    private fun mergeDone(state: ChatStreamingState): ChatStreamingMergeResult {
        var current = state
        var finalized = emptyList<SidePanelMessage>()

        if (current.streamingStatus == ChatStreamingStatus.Failed) {
            return ChatStreamingMergeResult(
                state = current.copy(
                    currentPartialText = "",
                    currentPartialThinking = "",
                    streamingThinkingId = null,
                    streamingAnswerId = null,
                    streamingOutputStreamId = null,
                ),
            )
        }

        if (current.streamingOutputStreamId != null) {
            val outputResult = finalizeActiveOutputStream(
                current,
                ChatOutputStreamStatus.COMPLETED,
                exitCode = null,
                durationMs = null,
            )
            current = outputResult.state
            finalized = outputResult.finalizedMessages
        }

        val thinkingId = resolveStreamingRowId(
            trackedId = current.streamingThinkingId,
            messages = current.messages,
            kind = SidePanelMessageKind.THINKING,
        )
        val answerId = resolveStreamingRowId(
            trackedId = current.streamingAnswerId,
            messages = current.messages,
            kind = SidePanelMessageKind.TEXT,
        )
        val messages = current.messages.map { message ->
            when (message.id) {
                answerId ->
                    message.copy(
                        content = ChatAssistantContentNormalizer.displayText(message.content),
                        isComplete = true
                    )
                thinkingId ->
                    message.copy(isComplete = true)
                else -> message
            }
        }
        val textFinalized = listOfNotNull(thinkingId, answerId)
            .mapNotNull { id -> messages.firstOrNull { it.id == id } }

        if (!hasAssistantResponseAfterLatestUser(messages)) {
            return ChatStreamingMergeResult(
                state = current.copy(
                    messages = stripAssistantTurn(messages),
                    currentPartialText = "",
                    currentPartialThinking = "",
                    streamingThinkingId = null,
                    streamingAnswerId = null,
                    streamingOutputStreamId = null,
                    streamingStatus = ChatStreamingStatus.Failed,
                    streamErrorMessage = EMPTY_RESPONSE_MESSAGE,
                ),
            )
        }

        return ChatStreamingMergeResult(
            state = current.copy(
                messages = messages,
                currentPartialText = "",
                currentPartialThinking = "",
                streamingThinkingId = null,
                streamingAnswerId = null,
                streamingOutputStreamId = null,
                streamingStatus = ChatStreamingStatus.Done
            ),
            finalizedMessages = finalized + textFinalized
        )
    }

    private fun hasAssistantResponseAfterLatestUser(messages: List<SidePanelMessage>): Boolean {
        val lastUserIndex = messages.indexOfLast { it.role == ChatMessageRole.USER }
        val responseStartIndex = if (lastUserIndex >= 0) lastUserIndex + 1 else 0
        return messages.drop(responseStartIndex).any(::isMeaningfulAssistantResponse)
    }

    private fun isMeaningfulAssistantResponse(message: SidePanelMessage): Boolean {
        return when (message.kind) {
            SidePanelMessageKind.OUTPUT_STREAM -> true
            SidePanelMessageKind.TEXT ->
                message.role == ChatMessageRole.ASSISTANT &&
                    ChatAssistantContentNormalizer.displayText(message.content).trim().isNotEmpty()
            SidePanelMessageKind.THINKING, SidePanelMessageKind.SYSTEM -> false
        }
    }

    private fun stripAssistantTurn(messages: List<SidePanelMessage>): List<SidePanelMessage> {
        val lastUserIndex = messages.indexOfLast { it.role == ChatMessageRole.USER }
        if (lastUserIndex < 0) {
            return messages.filter { it.role != ChatMessageRole.ASSISTANT }
        }
        return messages.take(lastUserIndex + 1)
    }

    private fun mergeError(state: ChatStreamingState, message: String): ChatStreamingMergeResult {
        var current = state
        var finalized = emptyList<SidePanelMessage>()
        if (current.streamingOutputStreamId != null) {
            val outputResult = finalizeActiveOutputStream(
                current,
                ChatOutputStreamStatus.FAILED,
                exitCode = null,
                durationMs = null,
            )
            current = outputResult.state
            finalized = outputResult.finalizedMessages
        }

        val cleanedMessages = current.messages.filter { row ->
            row.isComplete || row.role != ChatMessageRole.ASSISTANT
        }
        return ChatStreamingMergeResult(
            state = current.copy(
                messages = cleanedMessages,
                streamingStatus = ChatStreamingStatus.Failed,
                streamErrorMessage = message,
                currentPartialText = "",
                currentPartialThinking = "",
                streamingThinkingId = null,
                streamingAnswerId = null,
                streamingOutputStreamId = null,
            ),
            finalizedMessages = finalized,
        )
    }

    private fun resolveStreamingRowId(
        trackedId: UUID?,
        messages: List<SidePanelMessage>,
        kind: SidePanelMessageKind,
    ): UUID? {
        if (trackedId != null &&
            messages.any { it.id == trackedId && it.kind == kind }
        ) {
            return trackedId
        }
        return currentTurnMessages(messages).lastOrNull { message ->
            message.kind == kind &&
                message.role == ChatMessageRole.ASSISTANT &&
                !message.isComplete
        }?.id
    }

    private fun currentTurnMessages(messages: List<SidePanelMessage>): List<SidePanelMessage> {
        val lastUserIndex = messages.indexOfLast { it.role == ChatMessageRole.USER }
        return if (lastUserIndex < 0) {
            messages
        } else {
            messages.subList(lastUserIndex + 1, messages.size)
        }
    }
}
