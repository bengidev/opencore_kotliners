package io.github.bengidev.opencore.sidepanel.application

import io.github.bengidev.opencore.sidepanel.domain.SessionItem
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider

internal sealed interface SidePanelIntent {
    data object OnAppear : SidePanelIntent
    data class SessionsLoaded(val sessions: List<SessionItem>) : SidePanelIntent
    data class ApiKeyLoaded(val provider: SessionProvider, val key: String?) : SidePanelIntent
    data class SessionSelected(val id: String) : SidePanelIntent
    data class SessionRenamed(val id: String, val newTitle: String) : SidePanelIntent
    data class SessionDeleted(val id: String) : SidePanelIntent
    data object SettingsTapped : SidePanelIntent
    data object SettingsDismissed : SidePanelIntent
    data class ProviderSelected(val provider: SessionProvider) : SidePanelIntent
    data class ApiKeyChanged(val value: String) : SidePanelIntent
    data object SaveApiKeyTapped : SidePanelIntent
    data object RemoveApiKeyTapped : SidePanelIntent
    data class ApiKeySaved(val provider: SessionProvider) : SidePanelIntent
    data class ApiKeyRemoved(val provider: SessionProvider) : SidePanelIntent
}
