package io.github.bengidev.opencore.chat.application

internal object ChatReducer {
    fun reduce(state: ChatState, intent: ChatIntent): ChatState = when (intent) {
        ChatIntent.NewConversation -> state.copy(
            activeConversation = null,
            messages = emptyList(),
            isSending = false
        )
        is ChatIntent.ConversationOpened -> state.copy(
            activeConversation = intent.conversation,
            isSending = false
        )
        is ChatIntent.MessagesLoaded -> state.copy(messages = intent.messages)
        is ChatIntent.UserMessageAppended -> state.copy(
            messages = state.messages + intent.message
        )
        is ChatIntent.AssistantMessageAppended -> state.copy(
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
                state.copy(activeConversation = null, messages = emptyList(), isSending = false)
            }
        }
        ChatIntent.SendStarted -> state.copy(isSending = true)
        ChatIntent.SendFinished -> state.copy(isSending = false)
    }
}
