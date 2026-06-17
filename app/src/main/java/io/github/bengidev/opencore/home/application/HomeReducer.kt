package io.github.bengidev.opencore.home.application

internal object HomeReducer {
    fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        is HomeIntent.DraftMessageChanged -> state.copy(draftMessage = intent.value)
        HomeIntent.SendTapped -> state.copy(draftMessage = "")
        HomeIntent.AttachmentTapped,
        HomeIntent.ContextUsageTapped,
        HomeIntent.MicrophoneTapped,
        HomeIntent.ModelSelectorTapped,
        HomeIntent.NewConversationTapped,
        HomeIntent.SidebarTapped,
        HomeIntent.SpeedModeTapped -> state
    }
}
