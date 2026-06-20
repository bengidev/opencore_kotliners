package io.github.bengidev.opencore.sidepanel.application

import io.github.bengidev.opencore.sidepanel.domain.SessionItem

internal object SidePanelReducer {
    internal fun reduce(state: SidePanelState, intent: SidePanelIntent): SidePanelState = when (intent) {
        is SidePanelIntent.OnAppear -> state
        is SidePanelIntent.SessionsLoaded -> state.copy(sessions = intent.sessions)
        is SidePanelIntent.ApiKeyLoaded -> state.copy(
            selectedProvider = intent.provider,
            storedApiKey = intent.key,
            apiKeyInput = intent.key ?: ""
        )
        is SidePanelIntent.SessionSelected -> state.copy(
            sessions = state.sessions.map { it.copy(isActive = it.id == intent.id) }
        )
        is SidePanelIntent.SessionRenamed -> state.copy(
            sessions = state.sessions.map { if (it.id == intent.id) it.copy(title = intent.newTitle) else it }
        )
        is SidePanelIntent.SessionDeleted -> state.copy(
            sessions = state.sessions.filterNot { it.id == intent.id }
        )
        is SidePanelIntent.SettingsTapped -> state.copy(isSettingsVisible = true)
        is SidePanelIntent.SettingsDismissed -> state.copy(isSettingsVisible = false)
        is SidePanelIntent.ProviderSelected -> state.copy(selectedProvider = intent.provider)
        is SidePanelIntent.ApiKeyChanged -> state.copy(apiKeyInput = intent.value)
        is SidePanelIntent.SaveApiKeyTapped -> state
        is SidePanelIntent.RemoveApiKeyTapped -> state
        is SidePanelIntent.ApiKeySaved -> state.copy(
            storedApiKey = if (state.selectedProvider == intent.provider) state.apiKeyInput.takeIf { it.isNotBlank() } else state.storedApiKey,
            apiKeyInput = if (state.selectedProvider == intent.provider) state.apiKeyInput else state.apiKeyInput
        )
        is SidePanelIntent.ApiKeyRemoved -> state.copy(
            storedApiKey = if (state.selectedProvider == intent.provider) null else state.storedApiKey,
            apiKeyInput = if (state.selectedProvider == intent.provider) "" else state.apiKeyInput
        )
    }
}
