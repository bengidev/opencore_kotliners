package io.github.bengidev.opencore.sidepanel

import android.content.Context
import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.domain.CredentialStore
import io.github.bengidev.opencore.sidepanel.domain.SessionRepository
import io.github.bengidev.opencore.sidepanel.infrastructure.EncryptedCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySessionRepository

internal class SidePanelFacade(
    private val sessionRepositoryFactory: () -> SessionRepository = { InMemorySessionRepository() },
    private val credentialStoreFactory: (Context) -> CredentialStore = { EncryptedCredentialStore(it) }
) {
    fun createComponent(
        context: Context,
        componentContext: ComponentContext,
        onSettingsTappedCallback: () -> Unit = {}
    ): SidePanelComponent = SidePanelComponent(
        componentContext = componentContext,
        sessionRepository = sessionRepositoryFactory(),
        credentialStore = credentialStoreFactory(context.applicationContext),
        onSettingsTappedCallback = onSettingsTappedCallback
    )
}
