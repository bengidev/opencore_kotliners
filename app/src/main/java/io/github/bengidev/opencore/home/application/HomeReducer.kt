package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi

internal object HomeReducer {
    fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        is HomeIntent.DraftMessageChanged -> state.copy(draftMessage = intent.value)
        HomeIntent.SendTapped -> if (state.canSend) state.copy(draftMessage = "") else state
        HomeIntent.ModelSelectorTapped -> {
            val selectedIsPaid = state.availableModels
                .firstOrNull { it.id == state.selectedModelId }
                ?.isFree == false
            state.copy(
                isModelPickerVisible = true,
                modelSearchQuery = "",
                appliedSearchQuery = "",
                modelFilterFreeOnly = when {
                    state.selectedProviderId != SidePanelProviderApi.openRouter.id ->
                        state.modelFilterFreeOnly
                    selectedIsPaid -> false
                    else -> true
                }
            )
        }
        HomeIntent.ModelPickerDismissed -> state.copy(isModelPickerVisible = false)
        is HomeIntent.ModelSelected -> state.copy(
            selectedModelId = intent.model.id,
            selectedModelTitle = intent.model.displayTitle,
            selectedModelSupportsReasoning = intent.model.supportsReasoning,
            isModelPickerVisible = false
        )
        HomeIntent.ModelsLoadingStarted -> state.copy(isLoadingModels = true)
        is HomeIntent.ModelSelectionLoaded -> {
            val selectedModel = intent.models.firstOrNull { it.id == intent.modelId }
            state.copy(
                selectedModelId = intent.modelId,
                selectedModelTitle = intent.modelTitle,
                selectedModelSupportsReasoning = selectedModel?.supportsReasoning == true,
                selectedProviderId = intent.providerId,
                availableModels = intent.models,
                isLoadingModels = false,
                modelCatalogIsLive = intent.catalogIsLive,
                modelCatalogErrorHint = intent.catalogErrorHint
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
