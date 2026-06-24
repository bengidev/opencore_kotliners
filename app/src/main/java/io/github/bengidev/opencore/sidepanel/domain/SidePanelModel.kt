package io.github.bengidev.opencore.sidepanel.domain

internal data class SidePanelModel(
    val id: String,
    val displayTitle: String,
    val isFree: Boolean = false,
    val contextLength: Int? = null,
    val supportsReasoning: Boolean = false,
    val supportsSpeedModes: Boolean = false
)
