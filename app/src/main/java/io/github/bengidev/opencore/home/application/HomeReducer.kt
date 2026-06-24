package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi

internal object HomeReducer {
    fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        is HomeIntent.DraftMessageChanged -> state.copy(draftMessage = intent.value)
        HomeIntent.SendTapped -> if (state.canSend) state.copy(draftMessage = "") else state
        HomeIntent.ModelSelectorTapped -> state.copy(
            isModelPickerVisible = true,
            modelSearchQuery = "",
            appliedSearchQuery = "",
            modelFilterFreeOnly = state.selectedProviderId == SidePanelProviderApi.openRouter.id
        )
        HomeIntent.ModelPickerDismissed -> state.copy(isModelPickerVisible = false)
        is HomeIntent.ModelSelected -> state.copy(
            selectedModelId = intent.model.id,
            selectedModelTitle = intent.model.displayTitle,
            selectedModelSupportsReasoning = intent.model.supportsReasoning,
            isModelPickerVisible = false
        )
        is HomeIntent.ModelSelectionLoaded -> {
            val selectedModel = intent.models.firstOrNull { it.id == intent.modelId }
            state.copy(
                selectedModelId = intent.modelId,
                selectedModelTitle = intent.modelTitle,
                selectedModelSupportsReasoning = selectedModel?.supportsReasoning == true,
                selectedProviderId = intent.providerId,
                availableModels = intent.models
            )
        }
        is HomeIntent.CredentialsLoaded -> state.copy(
            hasApiKey = intent.hasApiKey,
            hasLoadedCredentials = true
        )
        is HomeIntent.ModelSearchQueryChanged -> state.copy(modelSearchQuery = intent.query)
        is HomeIntent.ModelSearchQueryApplied -> state.copy(appliedSearchQuery = intent.query)
        is HomeIntent.ModelFilterFreeOnlyChanged -> state.copy(modelFilterFreeOnly = intent.enabled)
        HomeIntent.AttachmentTapped,
        HomeIntent.ContextUsageTapped,
        HomeIntent.MicrophoneTapped,
        HomeIntent.SpeedModeTapped -> state
        HomeIntent.NewConversationTapped,
        HomeIntent.SidebarTapped -> state
    }
}
