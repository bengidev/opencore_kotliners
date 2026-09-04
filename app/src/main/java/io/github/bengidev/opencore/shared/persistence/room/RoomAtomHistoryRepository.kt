package io.github.bengidev.opencore.shared.persistence.room

import android.content.Context
import androidx.room.Room
import io.github.bengidev.opencore.atoms.domain.Atom
import io.github.bengidev.opencore.atoms.domain.AtomListEntry
import io.github.bengidev.opencore.chat.infrastructure.ChatTextMessageDetailCodec
import io.github.bengidev.opencore.chat.utilities.ChatAttachmentStore
import io.github.bengidev.opencore.chat.utilities.ChatVoiceAttachmentRetention
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryError
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryStoring
import io.github.bengidev.opencore.shared.persistence.session.AtomCompactionCheckpoint
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionContextBuilder
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntry
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntryKind
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionMessageCodec
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

internal class RoomAtomHistoryRepository(
    context: Context,
) : PersistenceAtomHistoryStoring {

    private val database: AtomDatabase = Room.databaseBuilder(
        context.applicationContext,
        AtomDatabase::class.java,
        "opencore_atoms.db",
    ).build()
    private val dao = database.atomDao()
    private val mutex = Mutex()

    override suspend fun listAtomEntries(): List<AtomListEntry> = mutex.withLock {
        val atoms = listAtomsUnlocked()
        atoms.map { atom ->
            val entries = loadSessionEntriesInternal(atom.id)
            val last = AtomSessionContextBuilder.lastListableMessage(entries)
            if (last != null) {
                AtomListEntry(atom, last.first, last.second)
            } else {
                val fallback = atom.title.trim().ifEmpty { "New atom" }
                AtomListEntry(atom, fallback, atom.updatedAt)
            }
        }.sortedWith(
            compareByDescending<AtomListEntry> { it.atom.isPinned }
                .thenByDescending { it.lastMessageAt },
        )
    }

    override suspend fun listAtoms(): List<Atom> = mutex.withLock {
        listAtomsUnlocked()
    }

    override suspend fun loadChatMessages(atomId: UUID): List<SidePanelMessage> = mutex.withLock {
        val entries = loadSessionEntriesInternal(atomId)
        AtomSessionContextBuilder.buildDisplayMessages(entries)
    }

    override suspend fun loadProjectedChatMessages(atomId: UUID): List<SidePanelMessage> = mutex.withLock {
        val entries = loadSessionEntriesInternal(atomId)
        val leafId = leafEntryIdInternal(atomId)
        AtomSessionContextBuilder.buildModelMessages(entries, leafId)
    }

    override suspend fun loadThreadDisplayMessages(atomId: UUID): List<SidePanelMessage> = mutex.withLock {
        val entries = loadSessionEntriesInternal(atomId)
        val leafId = leafEntryIdInternal(atomId)
        AtomSessionContextBuilder.buildThreadDisplayMessages(entries, leafId)
    }

    override suspend fun loadSessionEntries(atomId: UUID): List<AtomSessionEntry> = mutex.withLock {
        loadSessionEntriesInternal(atomId)
    }

    override suspend fun loadLeafEntryId(atomId: UUID): UUID? = mutex.withLock {
        leafEntryIdInternal(atomId)
    }

    override suspend fun saveAtom(atom: Atom) = mutex.withLock {
        dao.insertAtom(atomToEntity(atom))
    }

    override suspend fun appendChatMessage(atomId: UUID, message: SidePanelMessage) = mutex.withLock {
        val atomIdString = atomId.toString()
        if (dao.findAtom(atomIdString) == null) {
            throw PersistenceAtomHistoryError(atomId)
        }

        val existing = dao.findSessionEntry(message.id.toString())
        if (existing != null) {
            updateMessageRecord(existing, message)
            return
        }

        val parentId = dao.findAtom(atomIdString)?.leafEntryId
        val sortIndex = dao.maxSortIndex(atomIdString) + 1
        val payload = AtomSessionMessageCodec.encode(message)
        val record = SessionEntryEntity(
            id = message.id.toString(),
            atomId = atomIdString,
            parentId = parentId,
            kind = AtomSessionEntryKind.MESSAGE.name.lowercase(),
            payload = payload,
            timestamp = message.createdAt.toEpochMilli() / 1000.0,
            sortIndex = sortIndex,
        )
        dao.insertSessionEntry(record)
        dao.updateAtomLeaf(atomIdString, record.id, record.timestamp)
    }

    override suspend fun appendCompaction(atomId: UUID, checkpoint: AtomCompactionCheckpoint) =
        mutex.withLock {
            val atomIdString = atomId.toString()
            if (dao.findAtom(atomIdString) == null) {
                throw PersistenceAtomHistoryError(atomId)
            }

            val entryId = UUID.randomUUID()
            val parentId = dao.findAtom(atomIdString)?.leafEntryId
            val sortIndex = dao.maxSortIndex(atomIdString) + 1
            val payload = AtomSessionMessageCodec.encodeCompaction(checkpoint)
            val timestamp = Instant.now().epochSecond.toDouble()
            val record = SessionEntryEntity(
                id = entryId.toString(),
                atomId = atomIdString,
                parentId = parentId,
                kind = AtomSessionEntryKind.COMPACTION.name.lowercase(),
                payload = payload,
                timestamp = timestamp,
                sortIndex = sortIndex,
            )
            dao.insertSessionEntry(record)
            dao.updateAtomLeaf(atomIdString, record.id, timestamp)
        }

    override suspend fun replaceChatMessages(atomId: UUID, messages: List<SidePanelMessage>) =
        mutex.withLock {
            for (message in messages) {
                if (AtomSessionContextBuilder.isSyntheticCompactionSummary(message)) continue
                val exists = dao.findSessionEntry(message.id.toString()) != null
                if (exists) {
                    val existing = dao.findSessionEntry(message.id.toString())!!
                    updateMessageRecord(existing, message)
                } else {
                    appendChatMessageUnlocked(atomId, message)
                }
            }
        }

    override suspend fun deleteAtom(atomId: UUID) = mutex.withLock {
        val messages = loadChatMessagesUnlocked(atomId)
        val localPaths = messages.flatMap { message ->
            ChatTextMessageDetailCodec.decode(message.detailJson)
                .attachments
                .map { it.localPath }
        }
        ChatAttachmentStore.removeAll(localPaths)
        dao.deleteAtom(atomId.toString())
    }

    override suspend fun setPinned(atomId: UUID, isPinned: Boolean) = mutex.withLock {
        val atom = dao.findAtom(atomId.toString()) ?: return
        dao.updateAtom(atom.copy(isPinned = isPinned))
    }

    override suspend fun renameAtom(atomId: UUID, title: String) = mutex.withLock {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val atom = dao.findAtom(atomId.toString()) ?: return
        dao.updateAtom(
            atom.copy(
                title = trimmed,
                updatedAt = Instant.now().epochSecond.toDouble(),
            ),
        )
    }

    override suspend fun setGroup(atomId: UUID, groupName: String?) = mutex.withLock {
        val atom = dao.findAtom(atomId.toString()) ?: return
        val resolved = groupName?.trim()?.takeIf { it.isNotEmpty() }
        dao.updateAtom(atom.copy(groupName = resolved))
    }

    override suspend fun listGroups(): List<String> = mutex.withLock {
        dao.listAtoms()
            .mapNotNull { it.groupName }
            .distinct()
            .sorted()
    }

    override suspend fun pruneExpiredVoiceAttachments() = mutex.withLock {
        val cutoff = ChatVoiceAttachmentRetention.expirationCutoff()
        for (atom in listAtomsUnlocked()) {
            val entries = loadSessionEntriesInternal(atom.id)
            val messages = AtomSessionContextBuilder.buildDisplayMessages(entries)
            val (updated, removedPaths) = ChatVoiceAttachmentRetention.expireVoiceAttachments(messages, cutoff)
            if (removedPaths.isEmpty()) continue
            ChatAttachmentStore.removeAll(removedPaths)
            for (message in updated) {
                if (AtomSessionContextBuilder.isSyntheticCompactionSummary(message)) continue
                val existing = dao.findSessionEntry(message.id.toString())
                if (existing != null) {
                    updateMessageRecord(existing, message)
                }
            }
        }
    }

    suspend fun migrateFromDataStoreIfNeeded(
        legacyConversations: List<Atom>,
        legacyMessages: Map<UUID, List<SidePanelMessage>>,
    ) {
        mutex.withLock {
            val migrationKey = "datastore_history_migrated_v1"
            if (dao.metadataValue(migrationKey) == "1") return
            val existingIds = dao.listAtoms().map { it.id }.toSet()
            for (atom in legacyConversations) {
                if (atom.id.toString() in existingIds) continue
                dao.insertAtom(atomToEntity(atom))
                for (message in legacyMessages[atom.id].orEmpty()) {
                    appendChatMessageUnlocked(atom.id, message)
                }
            }
            dao.setMetadata(AppMetadataEntity(migrationKey, "1"))
        }
    }

    private suspend fun appendChatMessageUnlocked(atomId: UUID, message: SidePanelMessage) {
        val atomIdString = atomId.toString()
        val parentId = dao.findAtom(atomIdString)?.leafEntryId
        val sortIndex = dao.maxSortIndex(atomIdString) + 1
        val record = SessionEntryEntity(
            id = message.id.toString(),
            atomId = atomIdString,
            parentId = parentId,
            kind = AtomSessionEntryKind.MESSAGE.name.lowercase(),
            payload = AtomSessionMessageCodec.encode(message),
            timestamp = message.createdAt.toEpochMilli() / 1000.0,
            sortIndex = sortIndex,
        )
        dao.insertSessionEntry(record)
        dao.updateAtomLeaf(atomIdString, record.id, record.timestamp)
    }

    private suspend fun updateMessageRecord(existing: SessionEntryEntity, message: SidePanelMessage) {
        dao.updateSessionEntry(
            existing.copy(
                payload = AtomSessionMessageCodec.encode(message),
                timestamp = message.createdAt.toEpochMilli() / 1000.0,
            ),
        )
    }

    private suspend fun listAtomsUnlocked(): List<Atom> = dao.listAtoms().map(::atomFromEntity)

    private suspend fun loadChatMessagesUnlocked(atomId: UUID): List<SidePanelMessage> {
        val entries = loadSessionEntriesInternal(atomId)
        return AtomSessionContextBuilder.buildDisplayMessages(entries)
    }

    private suspend fun loadSessionEntriesInternal(atomId: UUID): List<AtomSessionEntry> =
        dao.listSessionEntries(atomId.toString()).map(::entryFromEntity)

    private suspend fun leafEntryIdInternal(atomId: UUID): UUID? =
        dao.findAtom(atomId.toString())?.leafEntryId?.let(UUID::fromString)

    private fun atomFromEntity(entity: AtomEntity): Atom =
        Atom(
            id = UUID.fromString(entity.id),
            title = entity.title,
            createdAt = Instant.ofEpochMilli((entity.createdAt * 1000).toLong()),
            updatedAt = Instant.ofEpochMilli((entity.updatedAt * 1000).toLong()),
            isPinned = entity.isPinned,
            groupName = entity.groupName,
        )

    private fun atomToEntity(atom: Atom): AtomEntity =
        AtomEntity(
            id = atom.id.toString(),
            title = atom.title,
            createdAt = atom.createdAt.epochSecond.toDouble(),
            updatedAt = atom.updatedAt.epochSecond.toDouble(),
            isPinned = atom.isPinned,
            groupName = atom.groupName,
            leafEntryId = null,
        )

    private fun entryFromEntity(entity: SessionEntryEntity): AtomSessionEntry {
        val atomId = UUID.fromString(entity.atomId)
        val entryId = UUID.fromString(entity.id)
        val parentId = entity.parentId?.let(UUID::fromString)
        val timestamp = Instant.ofEpochMilli((entity.timestamp * 1000).toLong())
        val kind = when (entity.kind.lowercase()) {
            "compaction" -> AtomSessionEntryKind.COMPACTION
            else -> AtomSessionEntryKind.MESSAGE
        }
        return when (kind) {
            AtomSessionEntryKind.MESSAGE -> AtomSessionEntry.messageEntry(
                id = entryId,
                atomId = atomId,
                parentId = parentId,
                message = AtomSessionMessageCodec.decode(entity.payload, entryId),
                timestamp = timestamp,
            )
            AtomSessionEntryKind.COMPACTION -> AtomSessionEntry.compactionEntry(
                id = entryId,
                atomId = atomId,
                parentId = parentId,
                checkpoint = AtomSessionMessageCodec.decodeCompaction(entity.payload),
                timestamp = timestamp,
            )
        }
    }
}
