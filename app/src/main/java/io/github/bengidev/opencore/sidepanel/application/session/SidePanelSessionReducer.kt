package io.github.bengidev.opencore.sidepanel.application.session

import java.time.Instant

internal object SidePanelSessionReducer {
    fun reduce(state: SidePanelSessionState, intent: SidePanelSessionIntent): SidePanelSessionState {
        return when (intent) {
            SidePanelSessionIntent.SidebarToggled ->
                state.copy(isSidebarVisible = !state.isSidebarVisible)
            SidePanelSessionIntent.SidebarDismissed ->
                state.copy(isSidebarVisible = false)
            is SidePanelSessionIntent.ConversationsLoaded ->
                state.copy(
                    conversations = SidePanelSessionState.deduplicatedPinnedFirst(intent.conversations),
                    availableGroups = intent.groups
                )
            is SidePanelSessionIntent.HistorySearchQueryChanged ->
                state.copy(historySearchQuery = intent.query)
            is SidePanelSessionIntent.ConversationPinToggled -> {
                val matching = state.conversations.indices.filter {
                    state.conversations[it].id == intent.conversation.id
                }
                if (matching.isEmpty()) state else {
                    val newValue = !state.conversations[matching.first()].isPinned
                    val updated = state.conversations.map { conversation ->
                        if (conversation.id == intent.conversation.id) {
                            conversation.copy(isPinned = newValue)
                        } else {
                            conversation
                        }
                    }
                    state.copy(conversations = SidePanelSessionState.deduplicatedPinnedFirst(updated))
                }
            }
            is SidePanelSessionIntent.ConversationRenamed -> {
                val trimmed = intent.title.trim()
                if (trimmed.isEmpty()) {
                    state
                } else {
                    val now = Instant.now()
                    val updated = state.conversations.map { conversation ->
                        if (conversation.id == intent.id) {
                            conversation.copy(title = trimmed, updatedAt = now)
                        } else {
                            conversation
                        }
                    }
                    state.copy(conversations = SidePanelSessionState.sortedPinnedFirst(updated))
                }
            }
            is SidePanelSessionIntent.ConversationDeleted ->
                state.copy(conversations = state.conversations.filter { it.id != intent.id })
            is SidePanelSessionIntent.ConversationGroupChanged -> {
                val normalizedGroup = intent.group?.trim()?.takeIf { it.isNotEmpty() }
                val expanded = if (normalizedGroup != null) {
                    state.expandedGroups + normalizedGroup
                } else {
                    state.expandedGroups
                }
                val updated = state.conversations.map { conversation ->
                    if (conversation.id == intent.id) {
                        conversation.copy(groupName = normalizedGroup)
                    } else {
                        conversation
                    }
                }
                state.copy(
                    expandedGroups = expanded,
                    conversations = SidePanelSessionState.sortedPinnedFirst(updated)
                )
            }
            is SidePanelSessionIntent.GroupHeaderToggled -> {
                val expanded = if (state.expandedGroups.contains(intent.group)) {
                    state.expandedGroups - intent.group
                } else {
                    state.expandedGroups + intent.group
                }
                state.copy(expandedGroups = expanded)
            }
            is SidePanelSessionIntent.ActiveConversationIdChanged ->
                state.copy(activeConversationId = intent.id)
        }
    }
}
