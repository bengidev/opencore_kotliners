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
            isModelPickerVisible = false
        )
        is HomeIntent.ModelSelectionLoaded -> state.copy(
            selectedModelId = intent.modelId,
            selectedModelTitle = intent.modelTitle,
            availableModels = intent.models
        )
        HomeIntent.AttachmentTapped,
        HomeIntent.ContextUsageTapped,
        HomeIntent.MicrophoneTapped,
        HomeIntent.SpeedModeTapped -> state
        HomeIntent.NewConversationTapped,
        HomeIntent.SidebarTapped -> state
    }
}
