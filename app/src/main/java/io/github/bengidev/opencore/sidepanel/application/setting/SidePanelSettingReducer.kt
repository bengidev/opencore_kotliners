package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort

internal object SidePanelSettingReducer {
    fun reduce(state: SidePanelSettingState, intent: SidePanelSettingIntent): SidePanelSettingState =
        when (intent) {
            is SidePanelSettingIntent.DraftChanged ->
                state.copy(draftApiKey = intent.value, errorMessage = null)
            is SidePanelSettingIntent.Appeared ->
                state.copy(
                    selectedProviderId = intent.selectedProviderId,
                    hasStoredKey = intent.hasStoredKey,
                    reasoningEffort = intent.reasoningEffort,
                    availableReasoningEfforts = intent.availableReasoningEfforts,
                    modelSupportsReasoning = intent.availableReasoningEfforts.isNotEmpty(),
                    errorMessage = null
                )
            SidePanelSettingIntent.SaveSucceeded ->
                state.copy(draftApiKey = "", hasStoredKey = true, errorMessage = null)
            SidePanelSettingIntent.ClearSucceeded ->
                state.copy(draftApiKey = "", hasStoredKey = false, errorMessage = null)
            is SidePanelSettingIntent.SaveFailed ->
                state.copy(errorMessage = intent.message)
            is SidePanelSettingIntent.ClearFailed ->
                state.copy(errorMessage = intent.message)
            is SidePanelSettingIntent.ProviderSelected ->
                state.copy(
                    selectedProviderId = intent.id,
                    hasStoredKey = intent.hasStoredKey,
                    errorMessage = null
                )
            is SidePanelSettingIntent.ReasoningEffortSelected ->
                state.copy(reasoningEffort = intent.effort)
            is SidePanelSettingIntent.ReasoningOptionsUpdated ->
                state.copy(
                    availableReasoningEfforts = intent.availableReasoningEfforts,
                    modelSupportsReasoning = intent.modelSupportsReasoning,
                    reasoningEffort = ModelReasoningEffort.resolvedSelection(
                        storedWireValue = state.reasoningEffort.wireValue,
                        available = intent.availableReasoningEfforts
                    )
                )
        }
}
