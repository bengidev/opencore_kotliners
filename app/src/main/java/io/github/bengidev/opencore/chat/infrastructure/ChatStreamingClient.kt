package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import kotlinx.coroutines.flow.Flow

/** Strategy seam for provider chat streaming. */
internal interface ChatStreamingClient {
    fun stream(
        messages: List<SidePanelMessage>,
        providerSortBy: String?,
        reasoningEffort: String?
    ): Flow<ChatStreamingEvent>
}
