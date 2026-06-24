package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel

internal interface SidePanelPreferenceStore {
    suspend fun preference(): SidePanelProviderPreference
    suspend fun setProviderId(id: String)
    suspend fun setModelId(id: String?)
    suspend fun setReasoningModel(model: SidePanelReasoningModel)
}
