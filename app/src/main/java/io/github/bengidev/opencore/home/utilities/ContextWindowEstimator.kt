package io.github.bengidev.opencore.home.utilities

import io.github.bengidev.opencore.chat.domain.ChatOutputStreamDetail
import io.github.bengidev.opencore.chat.utilities.ChatAssistantContentNormalizer
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind

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

    private fun messageText(message: SidePanelMessage): String = when (message.kind) {
        SidePanelMessageKind.OUTPUT_STREAM -> {
            if (!message.isComplete) {
                ""
            } else {
                val detail = ChatOutputStreamDetail.decode(message.detailJson, message.isComplete)
                message.content + "\n" + detail.outputTail
            }
        }
        else -> ChatAssistantContentNormalizer.displayText(message.content)
    }

    private fun estimatedTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return (trimmed.length + 3) / 4
    }
}
