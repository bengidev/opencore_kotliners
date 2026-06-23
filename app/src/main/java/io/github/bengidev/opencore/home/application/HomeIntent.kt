package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

internal sealed interface HomeIntent {
    data class DraftMessageChanged(val value: String) : HomeIntent
    data object SidebarTapped : HomeIntent
    data object NewConversationTapped : HomeIntent
    data object AttachmentTapped : HomeIntent
    data object MicrophoneTapped : HomeIntent
    data object SendTapped : HomeIntent
    data object ModelSelectorTapped : HomeIntent
    data object ModelPickerDismissed : HomeIntent
    data class ModelSelected(val model: SidePanelModel) : HomeIntent
    data class ModelSelectionLoaded(
        val modelId: String,
        val modelTitle: String,
        val models: List<SidePanelModel>
    ) : HomeIntent
    data class CredentialsLoaded(val hasApiKey: Boolean) : HomeIntent
    data object SpeedModeTapped : HomeIntent
    data object ContextUsageTapped : HomeIntent
}
