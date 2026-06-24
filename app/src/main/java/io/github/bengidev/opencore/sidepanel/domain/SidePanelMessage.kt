package io.github.bengidev.opencore.sidepanel.domain

import java.time.Instant
import java.util.UUID

/** Message type for history persistence boundary. */
internal data class SidePanelMessage(
    val id: UUID,
    val role: String,
    val content: String,
    val createdAt: Instant,
    val kind: SidePanelMessageKind = SidePanelMessageKind.TEXT,
    val isComplete: Boolean = true
)
