package io.github.bengidev.opencore.sidepanel.application.setting

import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.shared.providers.ProviderRegistry

internal data class SidePanelSettingState(
    val draftApiKey: String = "",
    val hasStoredKey: Boolean = false,
    val errorMessage: String? = null,
    val selectedProviderId: String = ProviderDescriptor.openRouter.id
) {
    val canSave: Boolean
        get() = draftApiKey.trim().isNotEmpty()

    val providerSupportsRouting: Boolean
        get() = ProviderRegistry.resolve(selectedProviderId).supportsProviderRouting
}
