package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModelCatalog
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class ProviderChatStreamingClient(
    private val preferenceStore: SidePanelPreferenceStore,
    private val credentialStore: SidePanelCredentialStore,
    private val delegate: OpenAiCompatibleStreamingClient = OpenAiCompatibleStreamingClient()
) : ChatStreamingClient {

    override fun stream(messages: List<SidePanelMessage>): Flow<ChatStreamingEvent> = flow {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val modelId = preference.modelId ?: SidePanelModelCatalog.defaultModel(provider).id
        val apiKey = credentialStore.secret(provider.id)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            emit(
                ChatStreamingEvent.Error(
                    ChatStreamError("Add an API key for ${provider.displayName} in Settings.")
                )
            )
            emit(ChatStreamingEvent.Done)
            return@flow
        }
        emitAll(
            delegate.stream(
                provider = provider,
                modelId = modelId,
                apiKey = apiKey,
                messages = messages,
                reasoning = preference.reasoningModel
            )
        )
    }
}
