package io.github.bengidev.opencore.sidepanel.application.session

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import java.util.UUID

internal sealed interface SidePanelSessionIntent {
    data object SidebarToggled : SidePanelSessionIntent
    data object SidebarDismissed : SidePanelSessionIntent
    data class ConversationsLoaded(
        val conversations: List<SidePanelConversation>,
        val groups: List<String>
    ) : SidePanelSessionIntent
    data class HistorySearchQueryChanged(val query: String) : SidePanelSessionIntent
    data class ConversationPinToggled(val conversation: SidePanelConversation) : SidePanelSessionIntent
    data class ConversationRenamed(val id: UUID, val title: String) : SidePanelSessionIntent
    data class ConversationDeleted(val id: UUID) : SidePanelSessionIntent
    data class ConversationGroupChanged(val id: UUID, val group: String?) : SidePanelSessionIntent
    data class GroupHeaderToggled(val group: String) : SidePanelSessionIntent
    data class ActiveConversationIdChanged(val id: UUID?) : SidePanelSessionIntent
}
