package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

internal data class ModelSelection(
    val modelId: String?,
    val modelTitle: String?
)

/**
 * Picks a catalog model from persisted preference and live provider models.
 * Stale saved IDs are always replaced; [autoSelectWhenNoneSaved] only applies when nothing is saved.
 */
internal object ModelSelectionPolicy {
    fun reconcile(
        models: List<SidePanelModel>,
        savedModelId: String?,
        autoSelectWhenNoneSaved: Boolean
    ): ModelSelection = when {
        models.isEmpty() -> ModelSelection(modelId = null, modelTitle = null)
        savedModelId != null && models.any { it.id == savedModelId } -> {
            val model = models.first { it.id == savedModelId }
            ModelSelection(modelId = model.id, modelTitle = model.displayTitle)
        }
        savedModelId != null -> {
            val model = models.first()
            ModelSelection(modelId = model.id, modelTitle = model.displayTitle)
        }
        autoSelectWhenNoneSaved -> {
            val model = models.first()
            ModelSelection(modelId = model.id, modelTitle = model.displayTitle)
        }
        else -> ModelSelection(modelId = null, modelTitle = null)
    }
}
