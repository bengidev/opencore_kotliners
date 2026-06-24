package io.github.bengidev.opencore.home.application

internal object HomeReducer {
    fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        is HomeIntent.DraftMessageChanged -> state.copy(draftMessage = intent.value)
        HomeIntent.SendTapped -> if (state.canSend) state.copy(draftMessage = "") else state
        HomeIntent.ModelSelectorTapped -> state.copy(isModelPickerVisible = true)
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
                availableModels = intent.models
            )
        }
        is HomeIntent.CredentialsLoaded -> state.copy(
            hasApiKey = intent.hasApiKey,
            hasLoadedCredentials = true
        )
        HomeIntent.AttachmentTapped,
        HomeIntent.ContextUsageTapped,
        HomeIntent.MicrophoneTapped,
        HomeIntent.SpeedModeTapped -> state
        HomeIntent.NewConversationTapped,
        HomeIntent.SidebarTapped -> state
    }
}
