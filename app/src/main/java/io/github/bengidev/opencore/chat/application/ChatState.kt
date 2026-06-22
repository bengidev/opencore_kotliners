package io.github.bengidev.opencore.chat.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

internal data class ChatState(
    val activeConversation: SidePanelConversation? = null,
    val messages: List<SidePanelMessage> = emptyList(),
    val isSending: Boolean = false
) {
    val isThreadActive: Boolean
        get() = activeConversation != null

    val headerTitle: String
        get() = activeConversation?.title.orEmpty()
}

internal sealed interface ChatIntent {
    data object NewConversation : ChatIntent
    data class ConversationOpened(val conversation: SidePanelConversation) : ChatIntent
    data class MessagesLoaded(val messages: List<SidePanelMessage>) : ChatIntent
    data class UserMessageAppended(val message: SidePanelMessage) : ChatIntent
    data class AssistantMessageAppended(val message: SidePanelMessage) : ChatIntent
    data class ActiveConversationRenamed(val id: UUID, val title: String) : ChatIntent
    data class ActiveConversationDeleted(val id: UUID) : ChatIntent
    data object SendStarted : ChatIntent
    data object SendFinished : ChatIntent
}
