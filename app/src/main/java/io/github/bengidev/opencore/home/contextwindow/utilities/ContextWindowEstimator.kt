package io.github.bengidev.opencore.home.contextwindow.utilities

import io.github.bengidev.opencore.home.contextwindow.models.ContextWindowUsage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

/** Character-based token estimation strategy until provider usage events land. */
internal object ContextWindowEstimator {
    fun estimate(
        messages: List<SidePanelMessage>,
        draft: String?,
        contextLength: Int?,
    ): ContextWindowUsage {
        var tokensUsed = messages.sumOf { estimatedTokens(messageText(it)) }
        if (draft != null) {
            tokensUsed += estimatedTokens(draft)
        }
        return ContextWindowUsage(
            tokensUsed = tokensUsed,
            tokenLimit = contextLength ?: 0,
        )
    }

    private fun messageText(message: SidePanelMessage): String = message.content

    private fun estimatedTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return (trimmed.length + 3) / 4
    }
}
