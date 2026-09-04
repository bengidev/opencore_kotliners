package io.github.bengidev.opencore.shared.persistence.session

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.infrastructure.attachments
import io.github.bengidev.opencore.chat.infrastructure.providerContent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import java.time.Instant
import java.util.UUID

/** Pi-style compaction-aware projection from append-only session entries to model context. */
internal object AtomSessionContextBuilder {
    const val COMPACTION_SUMMARY_PREFIX: String =
        "The conversation history before this point was compacted into the following summary:\n\n<summary>\n"
    const val COMPACTION_SUMMARY_SUFFIX: String = "\n</summary>"

    fun buildPath(entries: List<AtomSessionEntry>, leafId: UUID?): List<AtomSessionEntry> {
        if (entries.isEmpty()) return emptyList()
        val byId = entries.associateBy { it.id }
        val resolvedLeafId = leafId ?: entries.lastOrNull()?.id
        var current = resolvedLeafId?.let(byId::get) ?: return emptyList()

        val path = mutableListOf<AtomSessionEntry>()
        while (true) {
            path.add(current)
            val parentId = current.parentId ?: break
            current = byId[parentId] ?: break
        }
        return path.reversed()
    }

    fun buildContextEntries(entries: List<AtomSessionEntry>, leafId: UUID?): List<AtomSessionEntry> {
        val path = buildPath(entries, leafId)
        val compactionIndex = path.indexOfLast { it.kind == AtomSessionEntryKind.COMPACTION }
        if (compactionIndex < 0) return path

        val compaction = path[compactionIndex]
        val firstKeptEntryId = compaction.compaction?.firstKeptEntryId ?: return path

        val contextEntries = mutableListOf<AtomSessionEntry>()
        contextEntries.add(compaction)
        var foundFirstKept = false
        for (index in 0 until compactionIndex) {
            val entry = path[index]
            if (entry.id == firstKeptEntryId) {
                foundFirstKept = true
            }
            if (foundFirstKept) {
                contextEntries.add(entry)
            }
        }
        contextEntries.addAll(path.drop(compactionIndex + 1))
        return contextEntries
    }

    fun buildDisplayMessages(entries: List<AtomSessionEntry>): List<SidePanelMessage> =
        entries.mapNotNull { entry ->
            if (entry.kind == AtomSessionEntryKind.MESSAGE) entry.message else null
        }

    fun buildModelMessages(entries: List<AtomSessionEntry>, leafId: UUID?): List<SidePanelMessage> =
        buildContextEntries(entries, leafId).flatMap(::entryToModelMessages)

    fun entryToModelMessages(entry: AtomSessionEntry): List<SidePanelMessage> =
        when (entry.kind) {
            AtomSessionEntryKind.MESSAGE -> {
                val message = entry.message ?: return emptyList()
                listOf(message)
            }
            AtomSessionEntryKind.COMPACTION -> {
                val compaction = entry.compaction ?: return emptyList()
                val wrapped = COMPACTION_SUMMARY_PREFIX + compaction.summary + COMPACTION_SUMMARY_SUFFIX
                listOf(
                    SidePanelMessage(
                        id = entry.id,
                        role = ChatMessageRole.USER,
                        content = wrapped,
                        createdAt = entry.timestamp,
                    ),
                )
            }
        }

    fun listPreview(message: SidePanelMessage): String? {
        return when (message.kind) {
            SidePanelMessageKind.TEXT -> {
                val content = message.content.trim()
                if (content.startsWith(COMPACTION_SUMMARY_PREFIX.trim())) {
                    return "Compacted summary"
                }
                if (content.isNotEmpty()) return content
                if (message.attachments().isNotEmpty()) return "Attachment"
                null
            }
            SidePanelMessageKind.SYSTEM -> {
                val content = message.content.trim()
                if (content.isEmpty()) null else content
            }
            SidePanelMessageKind.OUTPUT_STREAM -> {
                val command = message.content.trim()
                if (command.isEmpty()) null else command
            }
            SidePanelMessageKind.THINKING -> null
        }
    }

    fun lastListableMessage(entries: List<AtomSessionEntry>): Pair<String, Instant>? {
        for (entry in entries.asReversed()) {
            if (entry.kind != AtomSessionEntryKind.MESSAGE) continue
            val message = entry.message ?: continue
            val preview = listPreview(message) ?: continue
            return preview to entry.timestamp
        }
        return null
    }

    fun isSyntheticCompactionSummary(message: SidePanelMessage): Boolean =
        message.kind == SidePanelMessageKind.TEXT &&
            message.content.startsWith(COMPACTION_SUMMARY_PREFIX)
}
