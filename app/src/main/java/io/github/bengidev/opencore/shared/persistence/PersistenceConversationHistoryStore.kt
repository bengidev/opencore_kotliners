package io.github.bengidev.opencore.shared.persistence

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

/** Facade over [PersistenceConversationHistoryStoring] for DI. */
internal class PersistenceConversationHistoryStore(
    private val delegate: PersistenceConversationHistoryStoring
) : PersistenceConversationHistoryStoring by delegate {

    companion object {
        val preview = PersistenceConversationHistoryStore(
            object : PersistenceConversationHistoryStoring {
                override suspend fun listConversations() = emptyList<SidePanelConversation>()
                override suspend fun loadMessages(conversationId: UUID) = emptyList<SidePanelMessage>()
                override suspend fun saveConversation(conversation: SidePanelConversation) = Unit
                override suspend fun appendMessage(conversationId: UUID, message: SidePanelMessage) = Unit
                override suspend fun deleteConversation(conversationId: UUID) = Unit
                override suspend fun setPinned(conversationId: UUID, isPinned: Boolean) = Unit
                override suspend fun renameConversation(conversationId: UUID, title: String) = Unit
                override suspend fun setGroup(conversationId: UUID, groupName: String?) = Unit
                override suspend fun listGroups() = emptyList<String>()
            }
        )
    }
}
