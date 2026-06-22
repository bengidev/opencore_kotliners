package io.github.bengidev.opencore.sidepanel.application.session

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

internal class SidePanelSessionComponent(
    componentContext: ComponentContext,
    private val history: SidePanelHistoryRepository,
    initialState: SidePanelSessionState = SidePanelSessionState()
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val _state = MutableValue(initialState)
    val state: Value<SidePanelSessionState> = _state

    var onOpenConversation: ((SidePanelConversation) -> Unit)? = null
    var onActiveConversationRenamed: ((UUID, String) -> Unit)? = null
    var onActiveConversationDeleted: ((UUID) -> Unit)? = null
    var onSettingsTapped: (() -> Unit)? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    fun dispatch(intent: SidePanelSessionIntent) {
        val priorActiveId = _state.value.activeConversationId
        _state.update { current -> SidePanelSessionReducer.reduce(current, intent) }

        when (intent) {
            is SidePanelSessionIntent.ConversationRenamed -> {
                val trimmed = intent.title.trim()
                if (trimmed.isNotEmpty() && intent.id == priorActiveId) {
                    onActiveConversationRenamed?.invoke(intent.id, trimmed)
                }
            }
            is SidePanelSessionIntent.ConversationDeleted -> {
                if (intent.id == priorActiveId) {
                    onActiveConversationDeleted?.invoke(intent.id)
                }
            }
            else -> Unit
        }
    }

    fun toggleSidebar() {
        dispatch(SidePanelSessionIntent.SidebarToggled)
        if (!_state.value.isSidebarVisible) return
        scope.launch {
            val conversations = history.listConversations()
            val groups = history.listGroups()
            dispatch(
                SidePanelSessionIntent.ConversationsLoaded(
                    conversations = conversations,
                    groups = groups
                )
            )
        }
    }

    fun dismissSidebar() = dispatch(SidePanelSessionIntent.SidebarDismissed)

    fun selectConversation(conversation: SidePanelConversation) {
        dispatch(SidePanelSessionIntent.SidebarDismissed)
        onOpenConversation?.invoke(conversation)
    }

    fun settingsButtonTapped() = onSettingsTapped?.invoke()

    fun onHistorySearchQueryChanged(query: String) =
        dispatch(SidePanelSessionIntent.HistorySearchQueryChanged(query))

    fun pinConversation(conversation: SidePanelConversation) {
        scope.launch {
            val currentValue = _state.value.conversations
                .firstOrNull { it.id == conversation.id }
                ?.isPinned ?: false
            dispatch(SidePanelSessionIntent.ConversationPinToggled(conversation))
            history.setPinned(conversation.id, !currentValue)
        }
    }

    fun renameConversation(id: UUID, title: String) {
        scope.launch {
            dispatch(SidePanelSessionIntent.ConversationRenamed(id, title))
            history.renameConversation(id, title)
        }
    }

    fun deleteConversation(id: UUID) {
        scope.launch {
            dispatch(SidePanelSessionIntent.ConversationDeleted(id))
            history.deleteConversation(id)
            val groups = history.listGroups()
            _state.update { current ->
                current.copy(availableGroups = groups)
            }
        }
    }

    fun changeGroup(id: UUID, group: String?) {
        scope.launch {
            dispatch(SidePanelSessionIntent.ConversationGroupChanged(id, group))
            history.setGroup(id, group)
            val groups = history.listGroups()
            _state.update { current ->
                current.copy(availableGroups = groups)
            }
        }
    }

    fun toggleGroupHeader(group: String) =
        dispatch(SidePanelSessionIntent.GroupHeaderToggled(group))
}
