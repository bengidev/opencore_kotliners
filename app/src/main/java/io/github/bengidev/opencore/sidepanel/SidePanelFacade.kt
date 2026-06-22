package io.github.bengidev.opencore.sidepanel

import android.content.Context
import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import io.github.bengidev.opencore.sidepanel.infrastructure.EncryptedSidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore

internal class SidePanelFacade {
    fun createComponent(
        context: Context,
        componentContext: ComponentContext,
        history: SidePanelHistoryRepository = InMemorySidePanelHistoryRepository(),
        credentialStore: SidePanelCredentialStore = EncryptedSidePanelCredentialStore(context),
        preferenceStore: SidePanelPreferenceStore = DataStoreSidePanelPreferenceStore(context)
    ): SidePanelComponent = SidePanelComponent(
        componentContext = componentContext,
        history = history,
        credentialStore = credentialStore,
        preferenceStore = preferenceStore
    )
}
