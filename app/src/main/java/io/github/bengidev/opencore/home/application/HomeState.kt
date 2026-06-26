package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.home.models.HomeComposerSpeedMode
import io.github.bengidev.opencore.home.models.HomeModelOption
import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.shared.providers.ProviderRegistry
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

internal data class HomeState(
    val draftMessage: String = "",
    val selectedModelId: String? = null,
    val selectedModelTitle: String = "Not Available",
    val reasoningEffortWireValue: String? = "high",
    val selectedProviderId: String = ProviderDescriptor.openRouter.id,
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

    val selectedModelOption: HomeModelOption?
        get() {
            val modelId = selectedModelId ?: return null
            val model = availableModels.firstOrNull { it.id == modelId } ?: return null
            return modelOptionFor(model)
        }

    val activeProviderSortBy: String?
        get() {
            val modes = selectedModelOption?.availableSpeedModes.orEmpty()
            if (modes.isEmpty()) return null
            return speedMode.providerSortBy
        }

    val activeReasoningEffort: String?
        get() {
            val option = selectedModelOption ?: return null
            if (option.availableReasoningEfforts.isEmpty()) return null
            return option.resolvedReasoningEffort(reasoningEffortWireValue).requestEffort
        }

    val selectedModelSupportsReasoning: Boolean
        get() = selectedModelOption?.supportsReasoning == true

    val selectedModelSupportsSpeedModes: Boolean
        get() = selectedModelOption?.availableSpeedModes?.isNotEmpty() == true

    fun modelOptionFor(model: SidePanelModel): HomeModelOption {
        val adapter = ProviderRegistry.resolve(selectedProviderId)
        return HomeModelOption(model = model, providerSupportsRouting = adapter.supportsProviderRouting)
    }

    val filteredModels: List<HomeModelOption>
        get() {
            var result = availableModels.map(::modelOptionFor)
            if (modelFilterFreeOnly) {
                result = result.filter { it.isFree }
            }
            val query = appliedSearchQuery.trim()
            if (query.isNotEmpty()) {
                result = result.filter { model ->
                    model.title.contains(query, ignoreCase = true) ||
                        model.id.contains(query, ignoreCase = true)
                }
            }
            return result
        }

    val isModelCatalogAvailable: Boolean
        get() = availableModels.isNotEmpty()

    val catalogHasFreeModels: Boolean
        get() = availableModels.any { it.isFree }

    val modelPickerTitle: String
        get() = if (isModelCatalogAvailable && selectedModelId != null) {
            selectedModelTitle
        } else {
            "Not Available"
        }
}
