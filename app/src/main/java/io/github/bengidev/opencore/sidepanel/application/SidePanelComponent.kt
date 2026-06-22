package io.github.bengidev.opencore.sidepanel.application

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.bengidev.opencore.sidepanel.application.session.SidePanelSessionComponent
import io.github.bengidev.opencore.sidepanel.application.session.SidePanelSessionState
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

internal class SidePanelComponent(
    componentContext: ComponentContext,
    history: SidePanelHistoryRepository,
    private val credentialStore: SidePanelCredentialStore,
    private val preferenceStore: SidePanelPreferenceStore
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val session: SidePanelSessionComponent = SidePanelSessionComponent(
        componentContext = childContext("session"),
        history = history
    )

    private var settingComponent: SidePanelSettingComponent? = null
    private val _showSettings = MutableValue(false)
    val showSettings: Value<Boolean> = _showSettings

    var modelSupportsReasoning: Boolean = false
        private set

    var selectedProviderId: String = SidePanelProviderApi.default.id
        private set

    val isSidebarVisible: Boolean
        get() = session.state.value.isSidebarVisible

    val sessionState: Value<SidePanelSessionState>
        get() = session.state

    val setting: SidePanelSettingComponent?
        get() = settingComponent

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

    var onCredentialsChanged: (() -> Unit)? = null
    var onReasoningModelChanged: (() -> Unit)? = null
    var onProviderChanged: ((String) -> Unit)? = null

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        session.onSettingsTapped = { settingsButtonTapped() }
        scope.launch { refreshSelectedProvider() }
    }

    fun toggleSidebar() = session.toggleSidebar()

    fun dismissSidebar() = session.dismissSidebar()

    fun settingsButtonTapped() {
        val component = SidePanelSettingComponent(
            componentContext = childContext("setting"),
            credentialStore = credentialStore,
            preferenceStore = preferenceStore,
            modelSupportsReasoning = modelSupportsReasoning,
        )
        component.onCredentialsChanged = {
            scope.launch { refreshSelectedProvider() }
            onCredentialsChanged?.invoke()
        }
        component.onReasoningModelChanged = {
            onReasoningModelChanged?.invoke()
        }
        component.onProviderChanged = { id ->
            scope.launch { refreshSelectedProvider() }
            onProviderChanged?.invoke(id)
        }
        settingComponent = component
        _showSettings.value = true
    }

    fun dismissSettings() {
        settingComponent = null
        _showSettings.value = false
        onCredentialsChanged?.invoke()
    }

    fun setModelSupportsReasoning(value: Boolean) {
        modelSupportsReasoning = value
    }

    private suspend fun refreshSelectedProvider() {
        selectedProviderId = preferenceStore.preference().providerId ?: SidePanelProviderApi.default.id
    }
}
