package io.github.bengidev.opencore.atoms.domain

import java.time.Instant
import java.util.UUID

/** Metadata for a persisted chat atom (formerly conversation). */
internal data class Atom(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val isPinned: Boolean = false,
    val groupName: String? = null,
)

/** Atom row with last-message preview for the Atoms list. */
internal data class AtomListEntry(
    val atom: Atom,
    val lastMessagePreview: String,
    val lastMessageAt: Instant,
)

internal fun Atom.toSidePanelConversation(): io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation =
    io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        groupName = groupName,
    )

internal fun io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation.toAtom(): Atom =
    Atom(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        groupName = groupName,
    )
