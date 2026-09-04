package io.github.bengidev.opencore.atoms.application

import io.github.bengidev.opencore.atoms.domain.Atom
import io.github.bengidev.opencore.atoms.domain.AtomListEntry
import java.util.UUID

internal sealed interface AtomsIntent {
    data class AtomsLoaded(
        val entries: List<AtomListEntry>,
        val groups: List<String>,
    ) : AtomsIntent

    data class SearchQueryChanged(val query: String) : AtomsIntent
    data class AtomPinToggled(val atom: Atom) : AtomsIntent
    data class AtomRenamed(val id: UUID, val title: String) : AtomsIntent
    data class AtomDeleted(val id: UUID) : AtomsIntent
    data class AtomGroupChanged(val id: UUID, val group: String?) : AtomsIntent
    data class GroupHeaderToggled(val group: String) : AtomsIntent
    data class ActiveAtomIdChanged(val id: UUID?) : AtomsIntent
}
