package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference

internal sealed interface SidePanelSettingIntent {
    data class DraftChanged(val value: String) : SidePanelSettingIntent
    data class Appeared(
        val selectedProviderId: String,
        val hasStoredKey: Boolean,
    ) : SidePanelSettingIntent
    data object SaveSucceeded : SidePanelSettingIntent
    data object ClearSucceeded : SidePanelSettingIntent
    data class SaveFailed(val message: String) : SidePanelSettingIntent
    data class ClearFailed(val message: String) : SidePanelSettingIntent
    data class ProviderSelected(val id: String, val hasStoredKey: Boolean) : SidePanelSettingIntent
    data class CompactionPreferenceLoaded(
        val preference: SettingsContextCompactionPreference,
    ) : SidePanelSettingIntent
    data class CompactionEnabledChanged(val enabled: Boolean) : SidePanelSettingIntent
    data class ThresholdPercentChanged(val percent: Int) : SidePanelSettingIntent
}
