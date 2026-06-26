package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
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
    val streamingStatus: ChatStreamingStatus = ChatStreamingStatus.Idle,
    val streamErrorMessage: String? = null
)

internal data class ChatStreamingMergeResult(
    val state: ChatStreamingState,
    val finalizedMessages: List<SidePanelMessage> = emptyList()
)

/** Pure merge logic for SSE stream events. */
internal object ChatStreamingMerger {
    fun applyPendingPartial(
        state: ChatStreamingState,
        partialThinking: String,
        partialText: String,
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
        ChatStreamingEvent.Done -> mergeDone(state)
        is ChatStreamingEvent.Error -> mergeError(state, event.error.message)
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
        val thinkingId = resolveStreamingRowId(
            trackedId = state.streamingThinkingId,
            messages = state.messages,
            kind = SidePanelMessageKind.THINKING,
        )
        val answerId = resolveStreamingRowId(
            trackedId = state.streamingAnswerId,
            messages = state.messages,
            kind = SidePanelMessageKind.TEXT,
        )
        val messages = state.messages.map { message ->
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
        val finalized = listOfNotNull(thinkingId, answerId)
            .mapNotNull { id -> messages.firstOrNull { it.id == id } }

        return ChatStreamingMergeResult(
            state = state.copy(
                messages = messages,
                currentPartialText = "",
                currentPartialThinking = "",
                streamingThinkingId = null,
                streamingAnswerId = null,
                streamingStatus = ChatStreamingStatus.Done
            ),
            finalizedMessages = finalized
        )
    }

    private fun mergeError(state: ChatStreamingState, message: String): ChatStreamingMergeResult {
        val cleanedMessages = state.messages.filter { row ->
            row.isComplete || row.role != ChatMessageRole.ASSISTANT
        }
        return ChatStreamingMergeResult(
            state.copy(
                messages = cleanedMessages,
                streamingStatus = ChatStreamingStatus.Failed,
                streamErrorMessage = message,
                currentPartialText = "",
                currentPartialThinking = "",
                streamingThinkingId = null,
                streamingAnswerId = null
            )
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
