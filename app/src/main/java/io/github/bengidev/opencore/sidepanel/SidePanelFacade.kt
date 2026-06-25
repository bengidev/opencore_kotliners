package io.github.bengidev.opencore.sidepanel

import android.content.Context
import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.DataStoreSidePanelPreferenceStore
import io.github.bengidev.opencore.shared.credential.CredentialEncryptedStore
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore

internal class SidePanelFacade {
    fun createComponent(
        context: Context,
        componentContext: ComponentContext,
        history: PersistenceConversationHistoryStoring = InMemorySidePanelHistoryRepository(),
        credentialStore: CredentialStoring = CredentialEncryptedStore(context),
        preferenceStore: SidePanelPreferenceStore = DataStoreSidePanelPreferenceStore(context)
    ): SidePanelComponent = SidePanelComponent(
        componentContext = componentContext,
        history = history,
        credentialStore = credentialStore,
        preferenceStore = preferenceStore
    )
}
