package io.github.bengidev.opencore.home.application

internal sealed interface HomeIntent {
    data class DraftMessageChanged(val value: String) : HomeIntent
    data object SidebarTapped : HomeIntent
    data object NewConversationTapped : HomeIntent
    data object AttachmentTapped : HomeIntent
    data object MicrophoneTapped : HomeIntent
    data object SendTapped : HomeIntent
    data object ModelSelectorTapped : HomeIntent
    data object SpeedModeTapped : HomeIntent
    data object ContextUsageTapped : HomeIntent
}
