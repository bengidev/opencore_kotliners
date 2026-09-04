package io.github.bengidev.opencore.atoms

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.atoms.application.AtomsComponent
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryStoring

internal class AtomsFacade {
    fun createComponent(
        componentContext: ComponentContext,
        history: PersistenceAtomHistoryStoring,
    ): AtomsComponent = AtomsComponent(
        componentContext = componentContext,
        history = history,
    )
}
