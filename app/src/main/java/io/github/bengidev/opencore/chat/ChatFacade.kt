package io.github.bengidev.opencore.chat

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.infrastructure.ProviderChatStreamingClient
import io.github.bengidev.opencore.chat.utilities.SettingsContextCompactionClient
import io.github.bengidev.opencore.chat.utilities.SettingsContextCompactionStreamSummarizer
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.shared.persistence.PersistenceAtomHistoryStoring
import io.github.bengidev.opencore.sidepanel.infrastructure.SettingsContextCompactionEngine
import io.github.bengidev.opencore.sidepanel.infrastructure.SettingsContextCompactionPreferenceStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore

/** Facade pattern: single entry point for the app shell to wire chat dependencies. */
internal class ChatFacade {
    fun createComponent(
        componentContext: ComponentContext,
        history: PersistenceAtomHistoryStoring,
        preferenceStore: SidePanelPreferenceStore,
        credentialStore: CredentialStoring,
        compactionPreferenceStore: SettingsContextCompactionPreferenceStore,
        contextLengthProvider: suspend () -> Int?,
    ): ChatComponent {
        val streamingClient = ProviderChatStreamingClient(
            preferenceStore = preferenceStore,
            credentialStore = credentialStore,
        )
        val summarizer = SettingsContextCompactionStreamSummarizer(
            streaming = streamingClient,
            preferenceStore = preferenceStore,
        )
        val compactionEngine = SettingsContextCompactionEngine(summarizer = summarizer)
        val compactionClient = SettingsContextCompactionClient.live(
            engine = compactionEngine,
            preferenceStore = compactionPreferenceStore,
        )
        return ChatComponent(
            componentContext = componentContext,
            history = history,
            streamingClient = streamingClient,
            contextCompaction = compactionClient,
            contextLengthProvider = contextLengthProvider,
        )
    }
}
