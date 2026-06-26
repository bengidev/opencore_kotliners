package io.github.bengidev.opencore.chat

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.infrastructure.ProviderChatStreamingClient
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore

/** Facade pattern: single entry point for the app shell to wire chat dependencies. */
internal class ChatFacade {
    fun createComponent(
        componentContext: ComponentContext,
        history: PersistenceConversationHistoryStoring,
        preferenceStore: SidePanelPreferenceStore,
        credentialStore: CredentialStoring
    ): ChatComponent = ChatComponent(
        componentContext = componentContext,
        history = history,
        streamingClient = ProviderChatStreamingClient(
            preferenceStore = preferenceStore,
            credentialStore = credentialStore
        )
    )
}
