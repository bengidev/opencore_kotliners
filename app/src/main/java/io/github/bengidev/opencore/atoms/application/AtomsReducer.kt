package io.github.bengidev.opencore.atoms.application

import java.time.Instant

internal object AtomsReducer {
    fun reduce(state: AtomsState, intent: AtomsIntent): AtomsState =
        when (intent) {
            is AtomsIntent.AtomsLoaded ->
                state.copy(
                    entries = AtomsState.deduplicatedPinnedFirst(intent.entries),
                    availableGroups = intent.groups,
                )
            is AtomsIntent.SearchQueryChanged ->
                state.copy(searchQuery = intent.query)
            is AtomsIntent.AtomPinToggled -> {
                val newValue = !(state.entries.firstOrNull { it.atom.id == intent.atom.id }
                    ?.atom?.isPinned ?: false)
                val updated = state.entries.map { entry ->
                    if (entry.atom.id == intent.atom.id) {
                        entry.copy(atom = entry.atom.copy(isPinned = newValue))
                    } else {
                        entry
                    }
                }
                state.copy(entries = AtomsState.deduplicatedPinnedFirst(updated))
            }
            is AtomsIntent.AtomRenamed -> {
                val trimmed = intent.title.trim()
                if (trimmed.isEmpty()) {
                    state
                } else {
                    val now = Instant.now()
                    val updated = state.entries.map { entry ->
                        if (entry.atom.id == intent.id) {
                            entry.copy(atom = entry.atom.copy(title = trimmed, updatedAt = now))
                        } else {
                            entry
                        }
                    }
                    state.copy(entries = updated)
                }
            }
            is AtomsIntent.AtomDeleted ->
                state.copy(entries = state.entries.filter { it.atom.id != intent.id })
            is AtomsIntent.AtomGroupChanged -> {
                val normalizedGroup = intent.group?.trim()?.takeIf { it.isNotEmpty() }
                val expanded = if (normalizedGroup != null) {
                    state.expandedGroups + normalizedGroup
                } else {
                    state.expandedGroups
                }
                val updated = state.entries.map { entry ->
                    if (entry.atom.id == intent.id) {
                        entry.copy(atom = entry.atom.copy(groupName = normalizedGroup))
                    } else {
                        entry
                    }
                }
                state.copy(expandedGroups = expanded, entries = updated)
            }
            is AtomsIntent.GroupHeaderToggled -> {
                val expanded = if (state.expandedGroups.contains(intent.group)) {
                    state.expandedGroups - intent.group
                } else {
                    state.expandedGroups + intent.group
                }
                state.copy(expandedGroups = expanded)
            }
            is AtomsIntent.ActiveAtomIdChanged ->
                state.copy(activeAtomId = intent.id)
        }
}
