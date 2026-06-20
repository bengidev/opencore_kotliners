package io.github.bengidev.opencore.sidepanel.application

import androidx.compose.runtime.Immutable
import io.github.bengidev.opencore.sidepanel.domain.SessionItem
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider

@Immutable
internal data class SidePanelState(
    val sessions: List<SessionItem> = emptyList(),
    val isSettingsVisible: Boolean = false,
    val selectedProvider: SessionProvider = SessionProvider.OpenRouter,
    val apiKeyInput: String = "",
    val storedApiKey: String? = null
) {
    val isKeyStored: Boolean get() = storedApiKey != null
}
