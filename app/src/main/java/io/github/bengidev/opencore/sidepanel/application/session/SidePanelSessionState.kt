package io.github.bengidev.opencore.sidepanel.application.session

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import java.util.UUID

internal data class SidePanelSessionState(
    val isSidebarVisible: Boolean = false,
    val conversations: List<SidePanelConversation> = emptyList(),
    val historySearchQuery: String = "",
    val activeConversationId: UUID? = null,
    val availableGroups: List<String> = emptyList(),
    val expandedGroups: Set<String> = emptySet()
) {
    val filteredConversations: List<SidePanelConversation>
        get() {
            val query = historySearchQuery.trim()
            val base = if (query.isEmpty()) {
                conversations
            } else {
                conversations.filter { it.title.contains(query, ignoreCase = true) }
            }
            return deduplicatedPinnedFirst(base)
        }

    companion object {
        fun sortedPinnedFirst(conversations: List<SidePanelConversation>): List<SidePanelConversation> =
            conversations.sortedWith(
                compareByDescending<SidePanelConversation> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )

        fun deduplicatedPinnedFirst(conversations: List<SidePanelConversation>): List<SidePanelConversation> {
            val sorted = sortedPinnedFirst(conversations)
            val seen = mutableSetOf<UUID>()
            return sorted.filter { seen.add(it.id) }
        }
    }
}
