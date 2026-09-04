package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference

internal class InMemorySettingsContextCompactionPreferenceStore(
    initial: SettingsContextCompactionPreference = SettingsContextCompactionPreference(),
) : SettingsContextCompactionPreferenceStore {
    private var current = initial

    override suspend fun preference(): SettingsContextCompactionPreference = current

    override suspend fun setEnabled(enabled: Boolean) {
        current = current.copy(isEnabled = enabled)
    }

    override suspend fun setReserveTokens(tokens: Int) {
        current = current.copy(reserveTokens = tokens)
    }

    override suspend fun setKeepRecentTokens(tokens: Int) {
        current = current.copy(keepRecentTokens = tokens)
    }

    override suspend fun setMinRecentMessages(count: Int) {
        current = current.copy(minRecentMessages = count)
    }
}
