package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi

internal data class HomeState(
    val draftMessage: String = "",
    val selectedModelId: String? = null,
    val selectedModelTitle: String = "Free Models Router",
    val selectedModelSupportsReasoning: Boolean = false,
    val selectedProviderId: String = SidePanelProviderApi.openRouter.id,
    val hasApiKey: Boolean = false,
    val hasLoadedCredentials: Boolean = false,
    val contextUsagePercent: Int = 41,
    val isModelPickerVisible: Boolean = false,
    val availableModels: List<SidePanelModel> = emptyList(),
    val modelSearchQuery: String = "",
    val appliedSearchQuery: String = "",
    val modelFilterFreeOnly: Boolean = false
) {
    val canSend: Boolean
        get() = draftMessage.isNotBlank() && selectedModelId != null && hasApiKey

    val showMissingApiKeyHint: Boolean
        get() = hasLoadedCredentials && !hasApiKey

    val filteredModels: List<SidePanelModel>
        get() {
            var result = availableModels
            if (modelFilterFreeOnly) {
                result = result.filter { it.isFree }
            }
            val query = appliedSearchQuery.trim()
            if (query.isNotEmpty()) {
                result = result.filter { model ->
                    model.displayTitle.contains(query, ignoreCase = true) ||
                        model.id.contains(query, ignoreCase = true)
                }
            }
            return result
        }
}
