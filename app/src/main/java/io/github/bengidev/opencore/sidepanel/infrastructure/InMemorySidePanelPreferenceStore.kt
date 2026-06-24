package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel

internal class InMemorySidePanelPreferenceStore(
    initial: SidePanelProviderPreference = SidePanelProviderPreference()
) : SidePanelPreferenceStore {
    private var current = initial

    override suspend fun preference(): SidePanelProviderPreference = current

    override suspend fun setProviderId(id: String) {
        current = current.copy(providerId = id)
    }

    override suspend fun setModelId(id: String?) {
        current = current.copy(modelId = id)
    }

    override suspend fun setReasoningModel(model: SidePanelReasoningModel) {
        current = current.copy(reasoningModel = model)
    }
}
