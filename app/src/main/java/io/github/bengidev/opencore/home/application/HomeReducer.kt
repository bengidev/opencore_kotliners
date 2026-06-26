package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort

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
            val option = state.modelOptionFor(intent.model)
            val speedMode = if (option.availableSpeedModes.contains(state.speedMode)) {
                state.speedMode
            } else {
                HomeComposerSpeedMode.STANDARD
            }
            val reasoningWire = option.resolvedReasoningEffort(state.reasoningEffortWireValue).wireValue
            state.copy(
                selectedModelId = intent.model.id,
                selectedModelTitle = intent.model.displayTitle,
                reasoningEffortWireValue = reasoningWire,
                speedMode = speedMode,
                isModelPickerVisible = false
            )
        }
        HomeIntent.ModelsLoadingStarted -> state.copy(isLoadingModels = true)
        is HomeIntent.ModelSelectionLoaded -> {
            val selectedModel = intent.modelId?.let { id ->
                intent.models.firstOrNull { it.id == id }
            }
            val option = selectedModel?.let { state.copy(selectedProviderId = intent.providerId).modelOptionFor(it) }
            val speedMode = if (option != null && option.availableSpeedModes.contains(state.speedMode)) {
                state.speedMode
            } else {
                HomeComposerSpeedMode.STANDARD
            }
            val reasoningWire = option?.resolvedReasoningEffort(state.reasoningEffortWireValue)?.wireValue
                ?: state.reasoningEffortWireValue
            state.copy(
                selectedModelId = intent.modelId,
                selectedModelTitle = intent.modelTitle ?: "Not Available",
                reasoningEffortWireValue = reasoningWire,
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
            val modes = state.selectedModelOption?.availableSpeedModes.orEmpty()
            if (intent.mode !in modes) state else state.copy(speedMode = intent.mode)
        }
        is HomeIntent.ReasoningEffortWireValueUpdated -> state.copy(reasoningEffortWireValue = intent.wireValue)
        is HomeIntent.ReasoningEffortSelected -> {
            val efforts = state.selectedModelOption?.availableReasoningEfforts.orEmpty()
            if (intent.effort !in efforts) state else state.copy(reasoningEffortWireValue = intent.effort.wireValue)
        }
        is HomeIntent.ContextUsageUpdated -> state.copy(contextUsage = intent.usage)
        HomeIntent.AttachmentTapped,
        HomeIntent.MicrophoneTapped -> state
        HomeIntent.NewConversationTapped,
        HomeIntent.SidebarTapped -> state
    }
}
