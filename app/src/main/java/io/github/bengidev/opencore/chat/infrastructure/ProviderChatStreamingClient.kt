package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.shared.credential.CredentialStoring
import io.github.bengidev.opencore.shared.providers.ProviderChatRequest
import io.github.bengidev.opencore.shared.providers.ProviderRegistry
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class ProviderChatStreamingClient(
    private val preferenceStore: SidePanelPreferenceStore,
    private val credentialStore: CredentialStoring,
    private val delegate: OpenAiCompatibleStreamingClient = OpenAiCompatibleStreamingClient()
) : ChatStreamingClient {

    override fun stream(
        messages: List<SidePanelMessage>,
        providerSortBy: String?,
        reasoningEffort: String?
    ): Flow<ChatStreamingEvent> = flow {
        val preference = preferenceStore.preference()
        val providerId = preference.providerId ?: ProviderRegistry.defaultAdapter.descriptor.id
        val adapter = ProviderRegistry.resolve(providerId)
        val apiKey = credentialStore.secret(providerId)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            emit(
                ChatStreamingEvent.Error(
                    ChatStreamError("Add an API key for ${adapter.descriptor.displayName} in Settings.")
                )
            )
            emit(ChatStreamingEvent.Done)
            return@flow
        }
        val modelId = preference.modelId
        if (modelId.isNullOrBlank()) {
            emit(
                ChatStreamingEvent.Error(
                    ChatStreamError("Select a model for ${adapter.descriptor.displayName} before sending.")
                )
            )
            emit(ChatStreamingEvent.Done)
            return@flow
        }
        val effort = reasoningEffort ?: preference.reasoningEffortWireValue
        emitAll(
            delegate.stream(
                providerId = providerId,
                modelId = modelId,
                apiKey = apiKey,
                messages = messages,
                reasoningEffort = effort,
                providerSortBy = providerSortBy
            )
        )
    }
}
