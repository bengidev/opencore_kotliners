package io.github.bengidev.opencore.atoms.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.atoms.domain.Atom
import io.github.bengidev.opencore.atoms.domain.toSidePanelConversation
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryStoring
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

internal class AtomsComponent(
    componentContext: ComponentContext,
    private val history: PersistenceAtomHistoryStoring,
    initialState: AtomsState = AtomsState(),
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(initialState)
    val state: Value<AtomsState> = _state

    var onOpenAtom: ((SidePanelConversation) -> Unit)? = null
    var onActiveAtomRenamed: ((UUID, String) -> Unit)? = null
    var onActiveAtomDeleted: ((UUID) -> Unit)? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        loadAtoms()
    }

    fun dispatch(intent: AtomsIntent) {
        val priorActiveId = _state.value.activeAtomId
        _state.update { current -> AtomsReducer.reduce(current, intent) }

        when (intent) {
            is AtomsIntent.AtomRenamed -> {
                val trimmed = intent.title.trim()
                if (trimmed.isNotEmpty() && intent.id == priorActiveId) {
                    onActiveAtomRenamed?.invoke(intent.id, trimmed)
                }
            }
            is AtomsIntent.AtomDeleted -> {
                if (intent.id == priorActiveId) {
                    onActiveAtomDeleted?.invoke(intent.id)
                }
            }
            else -> Unit
        }
    }

    fun loadAtoms() {
        scope.launch {
            val entries = history.listAtomEntries()
            val groups = history.listGroups()
            dispatch(AtomsIntent.AtomsLoaded(entries, groups))
        }
    }

    fun selectAtom(atom: Atom) {
        onOpenAtom?.invoke(atom.toSidePanelConversation())
    }

    fun pinAtom(atom: Atom) {
        scope.launch {
            val currentValue = _state.value.entries.firstOrNull { it.atom.id == atom.id }?.atom?.isPinned ?: false
            dispatch(AtomsIntent.AtomPinToggled(atom))
            history.setPinned(atom.id, !currentValue)
        }
    }

    fun renameAtom(id: UUID, title: String) {
        scope.launch {
            dispatch(AtomsIntent.AtomRenamed(id, title))
            history.renameAtom(id, title)
        }
    }

    fun syncAtomTitle(id: UUID, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        _state.update { current -> AtomsReducer.reduce(current, AtomsIntent.AtomRenamed(id, trimmed)) }
    }

    fun deleteAtom(id: UUID) {
        scope.launch {
            dispatch(AtomsIntent.AtomDeleted(id))
            history.deleteAtom(id)
            val groups = history.listGroups()
            _state.update { it.copy(availableGroups = groups) }
        }
    }

    fun changeGroup(id: UUID, group: String?) {
        scope.launch {
            dispatch(AtomsIntent.AtomGroupChanged(id, group))
            history.setGroup(id, group)
            val groups = history.listGroups()
            _state.update { it.copy(availableGroups = groups) }
        }
    }

    fun toggleGroupHeader(group: String) = dispatch(AtomsIntent.GroupHeaderToggled(group))

    fun setActiveAtomId(id: UUID?) = dispatch(AtomsIntent.ActiveAtomIdChanged(id))

    fun onSearchQueryChanged(query: String) = dispatch(AtomsIntent.SearchQueryChanged(query))

    fun refreshIfNeeded() = loadAtoms()
}
