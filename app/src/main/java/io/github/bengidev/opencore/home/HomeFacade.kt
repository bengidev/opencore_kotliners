package io.github.bengidev.opencore.home

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.home.application.HomeComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore

internal class HomeFacade {
    fun createComponent(
        componentContext: ComponentContext,
        preferenceStore: SidePanelPreferenceStore,
        onSendMessage: ((String) -> Unit)? = null,
        onNewConversation: (() -> Unit)? = null
    ): HomeComponent = HomeComponent(
        componentContext = componentContext,
        preferenceStore = preferenceStore,
        onSendMessage = onSendMessage,
        onNewConversation = onNewConversation
    )
}
