package io.github.bengidev.opencore.chat

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.chat.application.ChatComponent
import io.github.bengidev.opencore.chat.infrastructure.ChatCompletionClient
import io.github.bengidev.opencore.chat.infrastructure.EchoChatCompletionClient
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelHistoryRepository

/** Facade pattern: single entry point for the app shell to wire chat dependencies. */
internal class ChatFacade(
    private val completionClientFactory: () -> ChatCompletionClient = { EchoChatCompletionClient() }
) {
    fun createComponent(
        componentContext: ComponentContext,
        history: SidePanelHistoryRepository
    ): ChatComponent = ChatComponent(
        componentContext = componentContext,
        history = history,
        completionClient = completionClientFactory()
    )
}
