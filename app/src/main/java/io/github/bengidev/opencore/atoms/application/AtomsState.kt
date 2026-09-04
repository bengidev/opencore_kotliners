package io.github.bengidev.opencore.atoms.application

import io.github.bengidev.opencore.atoms.domain.AtomListEntry
import java.util.UUID

internal data class AtomsState(
    val entries: List<AtomListEntry> = emptyList(),
    val searchQuery: String = "",
    val activeAtomId: UUID? = null,
    val availableGroups: List<String> = emptyList(),
    val expandedGroups: Set<String> = emptySet(),
) {
    val filteredEntries: List<AtomListEntry>
        get() {
            val query = searchQuery.trim()
            val base = if (query.isEmpty()) {
                entries
            } else {
                entries.filter {
                    it.atom.title.contains(query, ignoreCase = true) ||
                        it.lastMessagePreview.contains(query, ignoreCase = true)
                }
            }
            return deduplicatedPinnedFirst(base)
        }

    companion object {
        fun deduplicatedPinnedFirst(entries: List<AtomListEntry>): List<AtomListEntry> {
            val sorted = entries.sortedWith(
                compareByDescending<AtomListEntry> { it.atom.isPinned }
                    .thenByDescending { it.lastMessageAt },
            )
            val seen = mutableSetOf<UUID>()
            return sorted.filter { seen.add(it.atom.id) }
        }
    }
}
