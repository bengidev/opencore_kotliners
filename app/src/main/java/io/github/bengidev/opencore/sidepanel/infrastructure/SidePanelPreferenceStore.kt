package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.shared.providers.ModelReasoningEffort
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference

internal interface SidePanelPreferenceStore {
    suspend fun preference(): SidePanelProviderPreference
    suspend fun setProviderId(id: String)
    suspend fun setModelId(id: String?)
    suspend fun setReasoningEffort(effort: ModelReasoningEffort)
}
