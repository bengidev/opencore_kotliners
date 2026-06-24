package io.github.bengidev.opencore.home

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore

internal class HomeFacade {
    fun createComponent(
        componentContext: ComponentContext,
        preferenceStore: SidePanelPreferenceStore,
        credentialStore: SidePanelCredentialStore,
        onSendMessage: ((String) -> Unit)? = null,
        onNewConversation: (() -> Unit)? = null
    ): HomeComponent = HomeComponent(
        componentContext = componentContext,
        preferenceStore = preferenceStore,
        credentialStore = credentialStore,
        onSendMessage = onSendMessage,
        onNewConversation = onNewConversation
    )
}
