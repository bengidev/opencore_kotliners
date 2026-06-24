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

        val thinkingId = state.streamingThinkingId
        return if (thinkingId != null) {
            val messages = state.messages.map { message ->
                if (message.id == thinkingId && message.kind == SidePanelMessageKind.THINKING) {
                    message.copy(content = partialThinking, isComplete = false)
                } else {
                    message
                }
            }
            ChatStreamingMergeResult(
                state.copy(
                    messages = messages,
                    currentPartialThinking = partialThinking,
                    streamingStatus = ChatStreamingStatus.Running
                )
            )
        } else {
            val newId = makeId()
            val thinkingMessage = SidePanelMessage(
                id = newId,
                role = ChatMessageRole.ASSISTANT,
                content = partialThinking,
                createdAt = now,
                kind = SidePanelMessageKind.THINKING,
                isComplete = false
            )
            ChatStreamingMergeResult(
                state.copy(
                    messages = state.messages + thinkingMessage,
                    currentPartialThinking = partialThinking,
                    streamingThinkingId = newId,
                    streamingStatus = ChatStreamingStatus.Running
                )
            )
        }
    }

    private fun mergeTextDelta(
        state: ChatStreamingState,
        delta: String,
        makeId: () -> UUID,
        now: Instant
    ): ChatStreamingMergeResult {
        val partialText = state.currentPartialText + delta
        val answerId = state.streamingAnswerId

        return if (answerId != null) {
            val messages = state.messages.map { message ->
                if (message.id == answerId && message.kind == SidePanelMessageKind.TEXT) {
                    message.copy(content = partialText, isComplete = false)
                } else {
                    message
                }
            }
            ChatStreamingMergeResult(
                state.copy(
                    messages = messages,
                    currentPartialText = partialText,
                    streamingStatus = ChatStreamingStatus.Running
                )
            )
        } else {
            val newId = makeId()
            val answerMessage = SidePanelMessage(
                id = newId,
                role = ChatMessageRole.ASSISTANT,
                content = partialText,
                createdAt = now,
                kind = SidePanelMessageKind.TEXT,
                isComplete = false
            )
            ChatStreamingMergeResult(
                state.copy(
                    messages = state.messages + answerMessage,
                    currentPartialText = partialText,
                    streamingAnswerId = newId,
                    streamingStatus = ChatStreamingStatus.Running
                )
            )
        }
    }

    private fun mergeDone(state: ChatStreamingState): ChatStreamingMergeResult {
        val messages = state.messages.map { message ->
            when (message.id) {
                state.streamingAnswerId ->
                    message.copy(
                        content = ChatAssistantContentNormalizer.displayText(message.content),
                        isComplete = true
                    )
                state.streamingThinkingId ->
                    message.copy(isComplete = true)
                else -> message
            }
        }
        val finalized = listOfNotNull(
            state.streamingThinkingId,
            state.streamingAnswerId
        ).mapNotNull { id -> messages.firstOrNull { it.id == id } }

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
            row.isComplete || (row.id != state.streamingThinkingId && row.id != state.streamingAnswerId)
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
}
