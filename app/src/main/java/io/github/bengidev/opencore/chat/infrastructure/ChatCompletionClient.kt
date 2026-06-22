package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

/** Strategy seam for provider chat completions. Streaming adapter replaces this later. */
internal fun interface ChatCompletionClient {
    suspend fun complete(messages: List<SidePanelMessage>): SidePanelMessage
}
