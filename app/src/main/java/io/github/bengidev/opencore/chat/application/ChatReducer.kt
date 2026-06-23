package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus

internal object ChatReducer {
    fun reduce(state: ChatState, intent: ChatIntent): ChatState = when (intent) {
        ChatIntent.NewConversation -> state.copy(
            activeConversation = null,
            messages = emptyList(),
            isSending = false,
            streamingStatus = ChatStreamingStatus.Idle,
            streamErrorMessage = null,
            currentPartialText = "",
            currentPartialThinking = "",
            streamingThinkingId = null,
            streamingAnswerId = null
        )
        is ChatIntent.ConversationOpened -> state.copy(
            activeConversation = intent.conversation,
            isSending = false,
            streamingStatus = ChatStreamingStatus.Idle,
            streamErrorMessage = null
        )
        is ChatIntent.MessagesLoaded -> state.copy(messages = intent.messages)
        is ChatIntent.UserMessageAppended -> state.copy(
            messages = state.messages + intent.message
        )
        is ChatIntent.ActiveConversationRenamed -> {
            if (state.activeConversation?.id != intent.id) {
                state
            } else {
                val trimmed = intent.title.trim()
                if (trimmed.isEmpty()) state else {
                    state.copy(
                        activeConversation = state.activeConversation.copy(title = trimmed)
                    )
                }
            }
        }
        is ChatIntent.ActiveConversationDeleted -> {
            if (state.activeConversation?.id != intent.id) {
                state
            } else {
                state.copy(
                    activeConversation = null,
                    messages = emptyList(),
                    isSending = false,
                    streamingStatus = ChatStreamingStatus.Idle,
                    streamErrorMessage = null
                )
            }
        }
        ChatIntent.StreamingTurnStarted -> state.copy(
            isSending = true,
            streamingStatus = ChatStreamingStatus.Running,
            currentPartialText = "",
            currentPartialThinking = "",
            streamErrorMessage = null,
            streamingThinkingId = null,
            streamingAnswerId = null
        )
        is ChatIntent.StreamingMerged -> {
            val stillSending = when (intent.result.state.streamingStatus) {
                ChatStreamingStatus.Running -> true
                ChatStreamingStatus.Done, ChatStreamingStatus.Failed -> false
                ChatStreamingStatus.Idle -> state.isSending
            }
            state.applyStreamingMerge(intent.result, isSending = stillSending)
        }
        ChatIntent.StreamingErrorDismissed -> state.copy(
            streamingStatus = ChatStreamingStatus.Idle,
            streamErrorMessage = null
        )
    }
}
