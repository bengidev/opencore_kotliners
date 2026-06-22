package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import java.time.Instant

import java.util.UUID

/** ponytail: echo stub until OpenAI-compatible streaming client lands. */
internal class EchoChatCompletionClient : ChatCompletionClient {
    override suspend fun complete(messages: List<SidePanelMessage>): SidePanelMessage {
        val lastUser = messages.lastOrNull { it.role == ChatMessageRole.USER }
        val content = lastUser?.content?.let { "Echo: $it" }.orEmpty()
        return SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.ASSISTANT,
            content = content,
            createdAt = Instant.now()
        )
    }
}
