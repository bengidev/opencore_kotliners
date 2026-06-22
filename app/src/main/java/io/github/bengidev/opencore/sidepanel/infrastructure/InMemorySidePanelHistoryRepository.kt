package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

internal class InMemorySidePanelHistoryRepository(
    seed: List<SidePanelConversation> = SidePanelConversation.previewSamples()
) : SidePanelHistoryRepository {
    private val conversations = linkedMapOf<UUID, SidePanelConversation>().apply {
        seed.forEach { put(it.id, it) }
    }
    private val messages = mutableMapOf<UUID, MutableList<SidePanelMessage>>()

    override suspend fun listConversations(): List<SidePanelConversation> =
        conversations.values
            .sortedWith(
                compareByDescending<SidePanelConversation> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
            .distinctBy { it.id }

    override suspend fun loadMessages(conversationId: UUID): List<SidePanelMessage> =
        messages[conversationId].orEmpty()

    override suspend fun saveConversation(conversation: SidePanelConversation) {
        conversations[conversation.id] = conversation
    }

    override suspend fun appendMessage(conversationId: UUID, message: SidePanelMessage) {
        val bucket = messages.getOrPut(conversationId) { mutableListOf() }
        val index = bucket.indexOfFirst { it.id == message.id }
        if (index >= 0) {
            bucket[index] = message
        } else {
            bucket += message
        }
        conversations[conversationId]?.let { existing ->
            conversations[conversationId] = existing.copy(updatedAt = message.createdAt)
        }
    }

    override suspend fun deleteConversation(conversationId: UUID) {
        conversations.remove(conversationId)
        messages.remove(conversationId)
    }

    override suspend fun setPinned(conversationId: UUID, isPinned: Boolean) {
        conversations[conversationId]?.let { existing ->
            conversations[conversationId] = existing.copy(isPinned = isPinned)
        }
    }

    override suspend fun renameConversation(conversationId: UUID, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        conversations[conversationId]?.let { existing ->
            conversations[conversationId] = existing.copy(
                title = trimmed,
                updatedAt = java.time.Instant.now()
            )
        }
    }

    override suspend fun setGroup(conversationId: UUID, groupName: String?) {
        val normalized = groupName?.trim()?.takeIf { it.isNotEmpty() }
        conversations[conversationId]?.let { existing ->
            conversations[conversationId] = existing.copy(groupName = normalized)
        }
    }

    override suspend fun listGroups(): List<String> =
        conversations.values.mapNotNull { it.groupName }.distinct().sorted()
}
