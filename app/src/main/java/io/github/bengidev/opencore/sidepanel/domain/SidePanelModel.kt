package io.github.bengidev.opencore.sidepanel.domain

internal data class SidePanelModel(
    val id: String,
    val displayTitle: String,
    val isFree: Boolean = false,
    val contextLength: Int? = null,
    val supportedReasoningEfforts: List<String> = emptyList(),
    val reasoningMandatory: Boolean = false,
    val supportsSpeedModes: Boolean = false,
    val supportsImageInput: Boolean = false,
    val supportsVideoInput: Boolean = false,
) {
    val supportsReasoning: Boolean
        get() = supportedReasoningEfforts.isNotEmpty()
}
