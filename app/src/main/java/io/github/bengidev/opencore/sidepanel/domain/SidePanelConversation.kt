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
) {
    companion object {
        private val previewNow: Instant = Instant.parse("2024-06-01T12:00:00Z")

        fun previewSamples(now: Instant = previewNow): List<SidePanelConversation> = listOf(
            SidePanelConversation(
                title = "Kotlin coroutines",
                createdAt = now.minusSeconds(3_600),
                updatedAt = now.minusSeconds(300)
            ),
            SidePanelConversation(
                title = "Compose side panel",
                createdAt = now.minusSeconds(86_400),
                updatedAt = now.minusSeconds(7_200)
            ),
            SidePanelConversation(
                title = "Pinned notes",
                createdAt = now.minusSeconds(172_800),
                updatedAt = now.minusSeconds(172_800),
                isPinned = true
            )
        )
    }
}
