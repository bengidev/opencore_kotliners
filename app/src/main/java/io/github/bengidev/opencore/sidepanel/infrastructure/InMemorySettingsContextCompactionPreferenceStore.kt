package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference

internal class InMemorySettingsContextCompactionPreferenceStore(
    initial: SettingsContextCompactionPreference = SettingsContextCompactionPreference()
        .normalizeAfterDecoding(),
) : SettingsContextCompactionPreferenceStore {
    private var current = initial

    override suspend fun preference(): SettingsContextCompactionPreference {
        val normalized = current.normalizeAfterDecoding()
        if (normalized != current) {
            current = normalized
        }
        return current
    }

    override suspend fun setEnabled(enabled: Boolean) {
        current = current.copy(isEnabled = enabled)
    }

    override suspend fun setThresholdPercent(percent: Int) {
        current = current.withThresholdPercent(percent)
    }

    override suspend fun setMinRecentMessages(count: Int) {
        current = current.copy(minRecentMessages = count)
    }
}
