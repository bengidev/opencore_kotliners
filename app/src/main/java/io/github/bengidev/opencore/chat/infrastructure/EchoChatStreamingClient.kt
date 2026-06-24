package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class EchoChatStreamingClient : ChatStreamingClient {
    override fun stream(messages: List<SidePanelMessage>, providerSortBy: String?): Flow<ChatStreamingEvent> = flow {
        val lastUser = messages.lastOrNull { it.role == ChatMessageRole.USER }
        val content = lastUser?.content.orEmpty()
        emit(ChatStreamingEvent.ThinkingDelta("Thinking about: $content"))
        emit(ChatStreamingEvent.TextDelta("Echo: $content"))
        emit(ChatStreamingEvent.Done)
    }
}
