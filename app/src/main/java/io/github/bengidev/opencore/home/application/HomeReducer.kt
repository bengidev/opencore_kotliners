package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
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
                    !state.catalogHasFreeModels -> false
                    selectedIsPaid -> false
                    else -> true
                }
            )
        }
        HomeIntent.ModelPickerDismissed -> state.copy(isModelPickerVisible = false)
        is HomeIntent.ModelSelected -> {
            val speedMode = if (intent.model.supportsSpeedModes &&
                state.speedMode in supportedSpeedModes(intent.model)
            ) {
                state.speedMode
            } else {
                HomeComposerSpeedMode.STANDARD
            }
            state.copy(
                selectedModelId = intent.model.id,
                selectedModelTitle = intent.model.displayTitle,
                selectedModelSupportsReasoning = intent.model.supportsReasoning,
                selectedModelSupportsSpeedModes = intent.model.supportsSpeedModes,
                speedMode = speedMode,
                isModelPickerVisible = false
            )
        }
        HomeIntent.ModelsLoadingStarted -> state.copy(isLoadingModels = true)
        is HomeIntent.ModelSelectionLoaded -> {
            val selectedModel = intent.modelId?.let { id ->
                intent.models.firstOrNull { it.id == id }
            }
            val speedMode = if (selectedModel?.supportsSpeedModes == true &&
                state.speedMode in supportedSpeedModes(selectedModel)
            ) {
                state.speedMode
            } else {
                HomeComposerSpeedMode.STANDARD
            }
            state.copy(
                selectedModelId = intent.modelId,
                selectedModelTitle = intent.modelTitle ?: "Not Available",
                selectedModelSupportsReasoning = selectedModel?.supportsReasoning == true,
                selectedModelSupportsSpeedModes = selectedModel?.supportsSpeedModes == true,
                selectedProviderId = intent.providerId,
                availableModels = intent.models,
                isLoadingModels = false,
                modelCatalogIsLive = intent.catalogIsLive,
                modelCatalogErrorHint = intent.catalogErrorHint,
                speedMode = speedMode
            )
        }
        HomeIntent.CatalogCleared -> state.copy(
            availableModels = emptyList(),
            modelCatalogIsLive = false,
            modelCatalogErrorHint = null
        )
        is HomeIntent.CredentialsLoaded -> state.copy(
            hasApiKey = intent.hasApiKey,
            hasLoadedCredentials = true
        )
        is HomeIntent.ModelSearchQueryChanged -> state.copy(modelSearchQuery = intent.query)
        is HomeIntent.ModelSearchQueryApplied -> state.copy(appliedSearchQuery = intent.query)
        is HomeIntent.ModelFilterFreeOnlyChanged -> state.copy(modelFilterFreeOnly = intent.enabled)
        is HomeIntent.SpeedModeSelected -> {
            if (!state.selectedModelSupportsSpeedModes ||
                intent.mode !in supportedSpeedModes(state)
            ) {
                state
            } else {
                state.copy(speedMode = intent.mode)
            }
        }
        is HomeIntent.ContextUsageUpdated -> state.copy(contextUsage = intent.usage)
        HomeIntent.AttachmentTapped,
        HomeIntent.ContextUsageTapped,
        HomeIntent.MicrophoneTapped,
        HomeIntent.SpeedModeTapped -> state
        HomeIntent.NewConversationTapped,
        HomeIntent.SidebarTapped -> state
    }

    private fun supportedSpeedModes(state: HomeState): Set<HomeComposerSpeedMode> =
        if (state.selectedModelSupportsSpeedModes) {
            setOf(HomeComposerSpeedMode.STANDARD, HomeComposerSpeedMode.FAST)
        } else {
            emptySet()
        }

    private fun supportedSpeedModes(model: io.github.bengidev.opencore.sidepanel.domain.SidePanelModel): Set<HomeComposerSpeedMode> =
        if (model.supportsSpeedModes) {
            setOf(HomeComposerSpeedMode.STANDARD, HomeComposerSpeedMode.FAST)
        } else {
            emptySet()
        }
}
