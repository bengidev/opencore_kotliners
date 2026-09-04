package io.github.bengidev.opencore.home.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.shared.tokenization.ContextTokenCounter
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

/** Token estimation backed by Tiktoken (`cl100k_base`) when available. */
internal object ContextWindowEstimator {
    fun estimate(
        messages: List<SidePanelMessage>,
        draft: String?,
        draftAttachments: List<ChatMessageAttachment> = emptyList(),
        contextLength: Int?,
    ): ContextWindowUsage {
        var tokensUsed = ContextTokenCounter.countTokens(messages, draft)
        if (draftAttachments.isNotEmpty()) {
            tokensUsed += io.github.bengidev.opencore.chat.utilities.ChatMultimodalWireLogic
                .estimatedWireTokenOverhead(draftAttachments)
        }
        return ContextWindowUsage(
            tokensUsed = tokensUsed,
            tokenLimit = contextLength ?: 0,
        )
    }

    fun shouldCompact(
        messages: List<SidePanelMessage>,
        draft: String?,
        contextLength: Int,
        reserveTokens: Int,
        triggerThresholdPercent: Int = 100,
    ): Boolean {
        if (contextLength <= 0) return false
        val tokensUsed = ContextTokenCounter.countTokens(messages, draft)
        val reserveThreshold = maxOf(0, contextLength - reserveTokens)
        val percentThreshold = (
            contextLength * triggerThresholdPercent.coerceIn(1, 100)
        ) / 100
        return tokensUsed > minOf(reserveThreshold, percentThreshold)
    }
}
