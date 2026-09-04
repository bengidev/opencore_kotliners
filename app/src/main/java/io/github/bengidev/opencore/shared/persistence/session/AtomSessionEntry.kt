package io.github.bengidev.opencore.shared.persistence.session

import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.Instant
import java.util.UUID

internal enum class AtomSessionEntryKind {
    MESSAGE,
    COMPACTION,
}

internal data class AtomCompactionCheckpoint(
    val summary: String,
    val firstKeptEntryId: UUID,
    val tokensBefore: Int,
    val readFiles: List<String> = emptyList(),
    val modifiedFiles: List<String> = emptyList(),
)

/** One node in an atom's append-only session tree. */
internal data class AtomSessionEntry(
    val id: UUID,
    val atomId: UUID,
    val parentId: UUID?,
    val kind: AtomSessionEntryKind,
    val timestamp: Instant,
    val message: SidePanelMessage? = null,
    val compaction: AtomCompactionCheckpoint? = null,
) {
    companion object {
        fun messageEntry(
            id: UUID,
            atomId: UUID,
            parentId: UUID?,
            message: SidePanelMessage,
            timestamp: Instant,
        ): AtomSessionEntry = AtomSessionEntry(
            id = id,
            atomId = atomId,
            parentId = parentId,
            kind = AtomSessionEntryKind.MESSAGE,
            timestamp = timestamp,
            message = message,
        )

        fun compactionEntry(
            id: UUID,
            atomId: UUID,
            parentId: UUID?,
            checkpoint: AtomCompactionCheckpoint,
            timestamp: Instant,
        ): AtomSessionEntry = AtomSessionEntry(
            id = id,
            atomId = atomId,
            parentId = parentId,
            kind = AtomSessionEntryKind.COMPACTION,
            timestamp = timestamp,
            compaction = checkpoint,
        )
    }
}
