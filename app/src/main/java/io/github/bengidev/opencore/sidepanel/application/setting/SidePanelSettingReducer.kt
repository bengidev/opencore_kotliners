package io.github.bengidev.opencore.sidepanel.application.setting

internal object SidePanelSettingReducer {
    fun reduce(state: SidePanelSettingState, intent: SidePanelSettingIntent): SidePanelSettingState =
        when (intent) {
            is SidePanelSettingIntent.DraftChanged ->
                state.copy(draftApiKey = intent.value, errorMessage = null)
            is SidePanelSettingIntent.Appeared ->
                state.copy(
                    selectedProviderId = intent.selectedProviderId,
                    hasStoredKey = intent.hasStoredKey,
                    reasoningModel = intent.reasoningModel,
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
            is SidePanelSettingIntent.ReasoningModelSelected ->
                state.copy(reasoningModel = intent.model)
        }
}
