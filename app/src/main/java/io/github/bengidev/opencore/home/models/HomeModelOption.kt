package io.github.bengidev.opencore.home.models

import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

internal data class HomeModelOption(
    val model: SidePanelModel,
    val availableSpeedModes: List<HomeComposerSpeedMode>,
    val availableReasoningEfforts: List<ModelReasoningEffort>
) {
    val id: String get() = model.id
    val title: String get() = model.displayTitle
    val isFree: Boolean get() = model.isFree
    val contextLength: Int? get() = model.contextLength
    val supportsImageInput: Boolean get() = model.supportsImageInput
    val supportsVideoInput: Boolean get() = model.supportsVideoInput
    val supportsFileInput: Boolean get() = model.supportsFileInput
    val supportsNonTextInput: Boolean
        get() = supportsImageInput || supportsVideoInput || supportsFileInput
    val supportsReasoning: Boolean get() = availableReasoningEfforts.isNotEmpty()

    constructor(
        model: SidePanelModel,
        providerSupportsRouting: Boolean = true
    ) : this(
        model = model,
        availableSpeedModes = if (model.supportsSpeedModes && providerSupportsRouting) {
            HomeComposerSpeedMode.entries
        } else {
            emptyList()
        },
        availableReasoningEfforts = ModelReasoningEffort.catalogOptions(
            wireEfforts = model.supportedReasoningEfforts,
            reasoningMandatory = model.reasoningMandatory
        )
    )

    fun resolvedReasoningEffort(storedWireValue: String?): ModelReasoningEffort =
        ModelReasoningEffort.resolvedSelection(storedWireValue, availableReasoningEfforts)
}
