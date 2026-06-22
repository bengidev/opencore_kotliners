package io.github.bengidev.opencore.sidepanel.domain

import java.time.Instant
import java.util.UUID

/**
 * Pure domain model for a conversation in the side panel.
 *
 * [groupName] places the conversation into a named folder in the history
 * sidebar. `null` means the conversation is ungrouped (appears in its
 * recency bucket).
 */
internal data class SidePanelConversation(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val isPinned: Boolean = false,
    val groupName: String? = null
)
