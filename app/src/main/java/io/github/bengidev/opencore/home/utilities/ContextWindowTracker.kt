package io.github.bengidev.opencore.home.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.home.models.ContextWindowUsage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

/** Facade that keeps the latest context window snapshot for a conversation. */
internal class ContextWindowTracker(
    var usage: ContextWindowUsage = ContextWindowUsage.zero,
) {
    fun refresh(
        messages: List<SidePanelMessage>,
        draft: String?,
        draftAttachments: List<ChatMessageAttachment> = emptyList(),
        contextLength: Int?,
    ) {
        usage = ContextWindowEstimator.estimate(
            messages = messages,
            draft = draft,
            draftAttachments = draftAttachments,
            contextLength = contextLength,
        )
    }
}
