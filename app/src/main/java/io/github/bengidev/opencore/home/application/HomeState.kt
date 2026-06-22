package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel

internal data class HomeState(
    val draftMessage: String = "",
    val selectedModelId: String? = null,
    val selectedModelTitle: String = "Free Models Router",
    val contextUsagePercent: Int = 41,
    val isModelPickerVisible: Boolean = false,
    val availableModels: List<SidePanelModel> = emptyList()
) {
    val canSend: Boolean
        get() = draftMessage.isNotBlank() && selectedModelId != null
}
