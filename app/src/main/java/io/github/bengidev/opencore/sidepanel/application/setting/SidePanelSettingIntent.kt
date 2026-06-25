package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort

internal sealed interface SidePanelSettingIntent {
    data class DraftChanged(val value: String) : SidePanelSettingIntent
    data class Appeared(
        val selectedProviderId: String,
        val hasStoredKey: Boolean,
        val reasoningEffort: ModelReasoningEffort,
        val availableReasoningEfforts: List<ModelReasoningEffort>
    ) : SidePanelSettingIntent
    data object SaveSucceeded : SidePanelSettingIntent
    data object ClearSucceeded : SidePanelSettingIntent
    data class SaveFailed(val message: String) : SidePanelSettingIntent
    data class ClearFailed(val message: String) : SidePanelSettingIntent
    data class ProviderSelected(val id: String, val hasStoredKey: Boolean) : SidePanelSettingIntent
    data class ReasoningEffortSelected(val effort: ModelReasoningEffort) : SidePanelSettingIntent
    data class ReasoningOptionsUpdated(
        val availableReasoningEfforts: List<ModelReasoningEffort>,
        val modelSupportsReasoning: Boolean
    ) : SidePanelSettingIntent
}
