package io.github.bengidev.opencore.sidepanel.domain

internal data class SidePanelProviderPreference(
    val providerId: String? = null,
    val reasoningModel: SidePanelReasoningModel = SidePanelReasoningModel.High
)
