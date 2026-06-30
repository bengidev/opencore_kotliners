package io.github.bengidev.opencore.home.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.utilities.ChatModelInputBuilder
import io.github.bengidev.opencore.chat.utilities.ChatMultimodalWireLogic
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.infrastructure.attachments
import io.github.bengidev.opencore.chat.infrastructure.providerContent
import io.github.bengidev.opencore.chat.utilities.ChatAssistantContentNormalizer
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind

/** Character-based token estimation strategy until provider usage events land. */
internal object ContextWindowEstimator {
    fun estimate(
        messages: List<SidePanelMessage>,
        draft: String?,
        draftAttachments: List<ChatMessageAttachment> = emptyList(),
        contextLength: Int?,
    ): ContextWindowUsage {
        var tokensUsed = messages.sumOf { messageTokens(it) }
        if (draft != null) {
            tokensUsed += estimatedTokens(
                ChatModelInputBuilder.modelContent(draft, draftAttachments),
            )
            tokensUsed += ChatMultimodalWireLogic.estimatedWireTokenOverhead(draftAttachments)
        }
        return ContextWindowUsage(
            tokensUsed = tokensUsed,
            tokenLimit = contextLength ?: 0,
        )
    }

    private fun messageTokens(message: SidePanelMessage): Int {
        val base = when (message.kind) {
            SidePanelMessageKind.OUTPUT_STREAM -> {
                val detail = ChatOutputStreamDetailCodec.decode(message.detailJson, message.isComplete)
                message.content + "\n" + detail.outputTail
            }
            else -> ChatAssistantContentNormalizer.displayText(message.providerContent())
        }
        return estimatedTokens(base) +
            ChatMultimodalWireLogic.estimatedWireTokenOverhead(message.attachments())
    }

    private fun estimatedTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return (trimmed.length + 3) / 4
    }
}
