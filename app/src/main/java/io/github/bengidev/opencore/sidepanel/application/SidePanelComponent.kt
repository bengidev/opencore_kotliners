package io.github.bengidev.opencore.sidepanel.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.childContext
import io.github.bengidev.opencore.sidepanel.application.session.SidePanelSessionComponent
import io.github.bengidev.opencore.sidepanel.application.session.SidePanelSessionState
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring
import java.util.UUID

internal class SidePanelComponent(
    componentContext: ComponentContext,
    history: PersistenceConversationHistoryStoring,
    val setting: SidePanelSettingComponent,
) : ComponentContext by componentContext {

    val session: SidePanelSessionComponent = SidePanelSessionComponent(
        componentContext = childContext("session"),
        history = history
    )

    private val _showSettings = MutableValue(false)
    val showSettings: Value<Boolean> = _showSettings

    val isSidebarVisible: Boolean
        get() = session.state.value.isSidebarVisible

    val sessionState: Value<SidePanelSessionState>
        get() = session.state

    var onOpenConversation: ((SidePanelConversation) -> Unit)? = null
        set(value) {
            field = value
            session.onOpenConversation = value
        }

    var onActiveConversationRenamed: ((UUID, String) -> Unit)? = null
        set(value) {
            field = value
            session.onActiveConversationRenamed = value
        }

    var onActiveConversationDeleted: ((UUID) -> Unit)? = null
        set(value) {
            field = value
            session.onActiveConversationDeleted = value
        }

    init {
        session.onSettingsTapped = { settingsButtonTapped() }
    }

    fun toggleSidebar() = session.toggleSidebar()

    fun dismissSidebar() = session.dismissSidebar()

    fun settingsButtonTapped() {
        session.dismissSidebar()
        setting.onAppear()
        _showSettings.value = true
    }

    fun dismissSettings() {
        _showSettings.value = false
    }
}
