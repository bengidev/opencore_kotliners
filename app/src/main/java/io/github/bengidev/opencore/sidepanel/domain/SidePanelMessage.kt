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
    val isComplete: Boolean = true,
    /** JSON-encoded detail for rich message kinds such as output streams. */
    val detailJson: String? = null,
) {
    val threadItemKey: String get() = "$id:$kind"
}

/** Keeps the latest row when persisted history contains duplicate ids. */
internal fun List<SidePanelMessage>.dedupeByMessageId(): List<SidePanelMessage> =
    reversed().distinctBy { it.id }.reversed()

/** Keeps the latest row when a thread list contains duplicate id/kind pairs. */
internal fun List<SidePanelMessage>.dedupeByThreadItemKey(): List<SidePanelMessage> =
    reversed().distinctBy { it.threadItemKey }.reversed()
