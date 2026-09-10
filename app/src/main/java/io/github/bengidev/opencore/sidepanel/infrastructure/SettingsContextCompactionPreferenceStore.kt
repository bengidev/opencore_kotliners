package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference

internal interface SettingsContextCompactionPreferenceStore {
    suspend fun preference(): SettingsContextCompactionPreference
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setThresholdPercent(percent: Int)
    suspend fun setMinRecentMessages(count: Int)
}
