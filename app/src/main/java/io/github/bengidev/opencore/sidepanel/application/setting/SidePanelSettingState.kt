package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel

internal data class SidePanelSettingState(
    val draftApiKey: String = "",
    val hasStoredKey: Boolean = false,
    val errorMessage: String? = null,
    val reasoningModel: SidePanelReasoningModel = SidePanelReasoningModel.High,
    val modelSupportsReasoning: Boolean = false,
    val selectedProviderId: String = SidePanelProviderApi.default.id
) {
    val canSave: Boolean
        get() = draftApiKey.trim().isNotEmpty()
}
