package io.github.bengidev.opencore.chat.presenter

internal object ChatReasoningCollapsePolicy {
    fun shouldAutoCollapse(
        hasCompetingStream: Boolean,
        isThinkingStreaming: Boolean,
    ): Boolean = isThinkingStreaming && hasCompetingStream
}
