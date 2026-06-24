package io.github.bengidev.opencore.sidepanel.domain

internal data class SidePanelModel(
    val id: String,
    val displayTitle: String,
    val supportsReasoning: Boolean = false
)
