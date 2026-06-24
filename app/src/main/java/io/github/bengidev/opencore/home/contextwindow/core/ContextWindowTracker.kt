package io.github.bengidev.opencore.home.contextwindow.core

import io.github.bengidev.opencore.home.contextwindow.models.ContextWindowUsage
import io.github.bengidev.opencore.home.contextwindow.utilities.ContextWindowEstimator
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

/** Facade that keeps the latest context window snapshot for a conversation. */
internal class ContextWindowTracker(
    var usage: ContextWindowUsage = ContextWindowUsage.zero,
) {
    fun refresh(
        messages: List<SidePanelMessage>,
        draft: String?,
        contextLength: Int?,
    ) {
        usage = ContextWindowEstimator.estimate(
            messages = messages,
            draft = draft,
            contextLength = contextLength,
        )
    }
}
