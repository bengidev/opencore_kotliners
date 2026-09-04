package io.github.bengidev.opencore.shared.persistence

import io.github.bengidev.opencore.atoms.domain.Atom
import io.github.bengidev.opencore.atoms.domain.AtomListEntry
import io.github.bengidev.opencore.atoms.domain.toAtom
import io.github.bengidev.opencore.shared.persistence.session.AtomCompactionCheckpoint
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionContextBuilder
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntry
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntryKind
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.Instant
import java.util.UUID

internal class InMemoryAtomHistoryRepository(
    seed: List<Atom> = emptyList(),
) : PersistenceAtomHistoryStoring {
    private val atoms = linkedMapOf<UUID, Atom>().apply {
        seed.forEach { put(it.id, it) }
    }
    private val sessionEntries = mutableMapOf<UUID, MutableList<AtomSessionEntry>>()

    override suspend fun listAtomEntries(): List<AtomListEntry> =
        listAtoms().map { atom ->
            val entries = loadSessionEntries(atom.id)
            val last = AtomSessionContextBuilder.lastListableMessage(entries)
            if (last != null) {
                AtomListEntry(atom, last.first, last.second)
            } else {
                AtomListEntry(atom, atom.title.ifBlank { "New atom" }, atom.updatedAt)
            }
        }

    override suspend fun listAtoms(): List<Atom> =
        atoms.values.sortedWith(
            compareByDescending<Atom> { it.isPinned }.thenByDescending { it.updatedAt },
        )

    override suspend fun loadChatMessages(atomId: UUID): List<SidePanelMessage> =
        AtomSessionContextBuilder.buildDisplayMessages(loadSessionEntries(atomId))

    override suspend fun loadProjectedChatMessages(atomId: UUID): List<SidePanelMessage> =
        AtomSessionContextBuilder.buildModelMessages(loadSessionEntries(atomId), loadLeafEntryId(atomId))

    override suspend fun loadSessionEntries(atomId: UUID): List<AtomSessionEntry> =
        sessionEntries[atomId].orEmpty()

    override suspend fun loadLeafEntryId(atomId: UUID): UUID? =
        sessionEntries[atomId]?.lastOrNull()?.id

    override suspend fun saveAtom(atom: Atom) {
        atoms[atom.id] = atom
    }

    override suspend fun appendChatMessage(atomId: UUID, message: SidePanelMessage) {
        val entries = sessionEntries.getOrPut(atomId) { mutableListOf() }
        val existingIndex = entries.indexOfFirst { it.id == message.id }
        if (existingIndex >= 0) {
            val existing = entries[existingIndex]
            entries[existingIndex] = existing.copy(message = message, timestamp = message.createdAt)
        } else {
            val parentId = entries.lastOrNull()?.id
            entries += AtomSessionEntry.messageEntry(
                id = message.id,
                atomId = atomId,
                parentId = parentId,
                message = message,
                timestamp = message.createdAt,
            )
        }
        atoms[atomId]?.let { existing ->
            atoms[atomId] = existing.copy(updatedAt = message.createdAt)
        }
    }

    override suspend fun replaceChatMessages(atomId: UUID, messages: List<SidePanelMessage>) {
        messages.forEach { appendChatMessage(atomId, it) }
    }

    override suspend fun appendCompaction(atomId: UUID, checkpoint: AtomCompactionCheckpoint) {
        val entries = sessionEntries.getOrPut(atomId) { mutableListOf() }
        val parentId = entries.lastOrNull()?.id
        entries += AtomSessionEntry.compactionEntry(
            id = UUID.randomUUID(),
            atomId = atomId,
            parentId = parentId,
            checkpoint = checkpoint,
            timestamp = Instant.now(),
        )
    }

    override suspend fun deleteAtom(atomId: UUID) {
        atoms.remove(atomId)
        sessionEntries.remove(atomId)
    }

    override suspend fun setPinned(atomId: UUID, isPinned: Boolean) {
        atoms[atomId]?.let { atoms[atomId] = it.copy(isPinned = isPinned) }
    }

    override suspend fun renameAtom(atomId: UUID, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        atoms[atomId]?.let { atoms[atomId] = it.copy(title = trimmed, updatedAt = Instant.now()) }
    }

    override suspend fun setGroup(atomId: UUID, groupName: String?) {
        val normalized = groupName?.trim()?.takeIf { it.isNotEmpty() }
        atoms[atomId]?.let { atoms[atomId] = it.copy(groupName = normalized) }
    }

    override suspend fun listGroups(): List<String> =
        atoms.values.mapNotNull { it.groupName }.distinct().sorted()

    override suspend fun pruneExpiredVoiceAttachments() = Unit

    suspend fun loadMessages(atomId: UUID): List<SidePanelMessage> = loadProjectedChatMessages(atomId)

    suspend fun saveConversation(conversation: SidePanelConversation) = saveAtom(conversation.toAtom())

    suspend fun appendMessage(conversationId: UUID, message: SidePanelMessage) =
        appendChatMessage(conversationId, message)
}
