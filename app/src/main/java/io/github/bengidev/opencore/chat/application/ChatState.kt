package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

internal data class ChatState(
    val activeConversation: SidePanelConversation? = null,
    val messages: List<SidePanelMessage> = emptyList(),
    val isSending: Boolean = false,
    val streamingStatus: ChatStreamingStatus = ChatStreamingStatus.Idle,
    val streamErrorMessage: String? = null,
    val currentPartialText: String = "",
    val currentPartialThinking: String = "",
    val streamingThinkingId: UUID? = null,
    val streamingAnswerId: UUID? = null
) {
    val isThreadActive: Boolean
        get() = activeConversation != null

    val headerTitle: String
        get() = activeConversation?.title.orEmpty()

    fun toStreamingState(): ChatStreamingState = ChatStreamingState(
        messages = messages,
        currentPartialText = currentPartialText,
        currentPartialThinking = currentPartialThinking,
        streamingThinkingId = streamingThinkingId,
        streamingAnswerId = streamingAnswerId,
        streamingStatus = streamingStatus,
        streamErrorMessage = streamErrorMessage
    )

    fun applyStreamingMerge(result: ChatStreamingMergeResult, isSending: Boolean): ChatState =
        copy(
            messages = result.state.messages,
            currentPartialText = result.state.currentPartialText,
            currentPartialThinking = result.state.currentPartialThinking,
            streamingThinkingId = result.state.streamingThinkingId,
            streamingAnswerId = result.state.streamingAnswerId,
            streamingStatus = result.state.streamingStatus,
            streamErrorMessage = result.state.streamErrorMessage,
            isSending = isSending
        )
}

internal sealed interface ChatIntent {
    data object NewConversation : ChatIntent
    data class ConversationOpened(val conversation: SidePanelConversation) : ChatIntent
    data class MessagesLoaded(val messages: List<SidePanelMessage>) : ChatIntent
    data class UserMessageAppended(val message: SidePanelMessage) : ChatIntent
    data class ActiveConversationRenamed(val id: UUID, val title: String) : ChatIntent
    data class ActiveConversationDeleted(val id: UUID) : ChatIntent
    data object StreamingTurnStarted : ChatIntent
    data class StreamingMerged(val result: ChatStreamingMergeResult) : ChatIntent
    data object StreamingErrorDismissed : ChatIntent
}
