package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

internal interface SidePanelHistoryRepository {
    suspend fun listConversations(): List<SidePanelConversation>
    suspend fun loadMessages(conversationId: UUID): List<SidePanelMessage>
    suspend fun saveConversation(conversation: SidePanelConversation)
    suspend fun appendMessage(conversationId: UUID, message: SidePanelMessage)
    suspend fun deleteConversation(conversationId: UUID)
    suspend fun setPinned(conversationId: UUID, isPinned: Boolean)
    suspend fun renameConversation(conversationId: UUID, title: String)
    suspend fun setGroup(conversationId: UUID, groupName: String?)
    suspend fun listGroups(): List<String>
}
