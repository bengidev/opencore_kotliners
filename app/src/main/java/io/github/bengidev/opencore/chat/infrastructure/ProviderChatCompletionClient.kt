package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModelCatalog
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelCredentialStore
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import java.time.Instant
import java.util.UUID

internal class ProviderChatCompletionClient(
    private val preferenceStore: SidePanelPreferenceStore,
    private val credentialStore: SidePanelCredentialStore,
    private val delegate: OpenAiCompatibleChatCompletionClient = OpenAiCompatibleChatCompletionClient()
) : ChatCompletionClient {

    override suspend fun complete(messages: List<SidePanelMessage>): SidePanelMessage {
        val preference = preferenceStore.preference()
        val provider = SidePanelProviderApi.resolve(preference.providerId)
        val modelId = preference.modelId ?: SidePanelModelCatalog.defaultModel(provider).id
        val apiKey = credentialStore.secret(provider.id)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return assistantMessage("Add an API key for ${provider.displayName} in Settings.")
        }
        return delegate.complete(
            provider = provider,
            modelId = modelId,
            apiKey = apiKey,
            messages = messages,
            reasoning = preference.reasoningModel
        )
    }

    private fun assistantMessage(content: String): SidePanelMessage =
        SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.ASSISTANT,
            content = content,
            createdAt = Instant.now()
        )
}
