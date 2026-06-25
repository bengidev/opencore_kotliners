package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi

internal data class HomeState(
    val draftMessage: String = "",
    val selectedModelId: String? = null,
    val selectedModelTitle: String = "Not Available",
    val selectedModelSupportsReasoning: Boolean = false,
    val selectedModelSupportsSpeedModes: Boolean = false,
    val selectedProviderId: String = SidePanelProviderApi.openRouter.id,
    val hasApiKey: Boolean = false,
    val hasLoadedCredentials: Boolean = false,
    val contextUsage: ContextWindowUsage = ContextWindowUsage.zero,
    val speedMode: HomeComposerSpeedMode = HomeComposerSpeedMode.STANDARD,
    val isModelPickerVisible: Boolean = false,
    val availableModels: List<SidePanelModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelCatalogIsLive: Boolean = false,
    val modelCatalogErrorHint: String? = null,
    val modelSearchQuery: String = "",
    val appliedSearchQuery: String = "",
    val modelFilterFreeOnly: Boolean = false
) {
    val canSend: Boolean
        get() = draftMessage.isNotBlank() &&
            selectedModelId != null &&
            hasApiKey &&
            !isLoadingModels &&
            availableModels.any { it.id == selectedModelId }

    val showMissingApiKeyHint: Boolean
        get() = hasLoadedCredentials && !hasApiKey

    val activeProviderSortBy: String?
        get() = if (selectedModelSupportsSpeedModes) speedMode.providerSortBy else null

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

    /** True when the provider catalog has been loaded and offers at least one model. */
    val isModelCatalogAvailable: Boolean
        get() = availableModels.isNotEmpty()

    /** True when the loaded catalog includes at least one free-tier model. */
    val catalogHasFreeModels: Boolean
        get() = availableModels.any { it.isFree }

    /** Composer model chip label; shows "Not Available" when the catalog is empty. */
    val modelPickerTitle: String
        get() = if (isModelCatalogAvailable && selectedModelId != null) {
            selectedModelTitle
        } else {
            "Not Available"
        }
}
