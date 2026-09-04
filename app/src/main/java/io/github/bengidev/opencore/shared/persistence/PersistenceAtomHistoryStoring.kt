package io.github.bengidev.opencore.shared.persistence

import io.github.bengidev.opencore.atoms.domain.Atom
import io.github.bengidev.opencore.atoms.domain.AtomListEntry
import io.github.bengidev.opencore.shared.persistence.session.AtomCompactionCheckpoint
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntry
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.util.UUID

/** Repository contract for atom history with append-only session trees. */
internal interface PersistenceAtomHistoryStoring {
    suspend fun listAtomEntries(): List<AtomListEntry>
    suspend fun listAtoms(): List<Atom>
    suspend fun loadChatMessages(atomId: UUID): List<SidePanelMessage>
    suspend fun loadProjectedChatMessages(atomId: UUID): List<SidePanelMessage>
    suspend fun loadSessionEntries(atomId: UUID): List<AtomSessionEntry>
    suspend fun loadLeafEntryId(atomId: UUID): UUID?
    suspend fun saveAtom(atom: Atom)
    suspend fun appendChatMessage(atomId: UUID, message: SidePanelMessage)
    suspend fun replaceChatMessages(atomId: UUID, messages: List<SidePanelMessage>)
    suspend fun appendCompaction(atomId: UUID, checkpoint: AtomCompactionCheckpoint)
    suspend fun deleteAtom(atomId: UUID)
    suspend fun setPinned(atomId: UUID, isPinned: Boolean)
    suspend fun renameAtom(atomId: UUID, title: String)
    suspend fun setGroup(atomId: UUID, groupName: String?)
    suspend fun listGroups(): List<String>
    suspend fun pruneExpiredVoiceAttachments()
}

internal class PersistenceAtomHistoryError(val atomId: UUID) : Exception("Atom not found: $atomId")
