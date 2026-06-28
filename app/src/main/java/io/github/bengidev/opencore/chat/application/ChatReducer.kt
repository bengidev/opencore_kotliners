package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.chat.domain.ChatStreamingStatus
import io.github.bengidev.opencore.sidepanel.domain.dedupeByThreadItemKey

internal object ChatReducer {
    fun reduce(state: ChatState, intent: ChatIntent): ChatState = when (intent) {
        ChatIntent.NewConversation -> state.clearedStreamingFields().copy(
            activeConversation = null,
            messages = emptyList(),
            isLoadingMessages = false
        )
        is ChatIntent.ConversationOpened -> if (intent.loadMessages) {
            state.clearedStreamingFields().copy(
                activeConversation = intent.conversation,
                messages = emptyList(),
                isLoadingMessages = true
            )
        } else {
            state.clearedStreamingFields().copy(
                activeConversation = intent.conversation,
                isLoadingMessages = false
            )
        }
        is ChatIntent.MessagesLoaded -> {
            if (state.activeConversation?.id != intent.conversationId) {
                state
            } else {
                state.clearedStreamingFields().copy(
                    messages = intent.messages.dedupeByThreadItemKey(),
                    isLoadingMessages = false
                )
            }
        }
        is ChatIntent.UserMessageAppended -> state.copy(
            messages = (state.messages + intent.message).dedupeByThreadItemKey()
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
                state.clearedStreamingFields().copy(
                    activeConversation = null,
                    messages = emptyList(),
                    isLoadingMessages = false
                )
            }
        }
        ChatIntent.StreamingTurnStarted -> state
            .withoutIncompleteAssistantRows()
            .copy(
                isSending = true,
                streamingStatus = ChatStreamingStatus.Running,
                currentPartialText = "",
                currentPartialThinking = "",
                streamErrorMessage = null,
                streamingThinkingId = null,
                streamingAnswerId = null,
                streamingOutputStreamId = null,
                streamingRevision = 0
            )
        is ChatIntent.StreamingMerged -> {
            val stillSending = when (intent.result.state.streamingStatus) {
                ChatStreamingStatus.Running -> true
                ChatStreamingStatus.Done, ChatStreamingStatus.Failed -> false
                ChatStreamingStatus.Idle -> state.isSending
            }
            state.applyStreamingMerge(
                intent.result,
                isSending = stillSending,
                bumpStreamingRevision = intent.bumpStreamingRevision
            )
        }
        ChatIntent.StreamingErrorDismissed -> state
            .withoutIncompleteAssistantRows()
            .clearedStreamingFields()
    }
}
