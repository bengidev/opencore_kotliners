package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.contextwindow.models.ContextWindowUsage
import io.github.bengidev.opencore.home.speedmode.models.HomeComposerSpeedMode
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
        val modelId: String?,
        val modelTitle: String?,
        val providerId: String,
        val models: List<SidePanelModel>,
        val catalogIsLive: Boolean = false,
        val catalogErrorHint: String? = null
    ) : HomeIntent
    data object CatalogCleared : HomeIntent
    data object ModelsLoadingStarted : HomeIntent
    data class CredentialsLoaded(val hasApiKey: Boolean) : HomeIntent
    data class ModelSearchQueryChanged(val query: String) : HomeIntent
    data class ModelSearchQueryApplied(val query: String) : HomeIntent
    data class ModelFilterFreeOnlyChanged(val enabled: Boolean) : HomeIntent
    data object SpeedModeTapped : HomeIntent
    data class SpeedModeSelected(val mode: HomeComposerSpeedMode) : HomeIntent
    data object ContextUsageTapped : HomeIntent
    data class ContextUsageUpdated(val usage: ContextWindowUsage) : HomeIntent
}
