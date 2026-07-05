package io.github.bengidev.opencore.chat.presenter

import io.github.bengidev.opencore.chat.application.ChatState
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind

internal object ChatCompetingStreamPolicy {
    /** True when an answer, command output, or buffered answer text is actively streaming. */
    fun hasCompetingStream(state: ChatState): Boolean {
        if (state.streamingAnswerId != null || state.streamingOutputStreamId != null) return true
        if (state.isSending && state.currentPartialText.isNotEmpty()) return true
        if (!state.isSending) return false
        return state.messages.any { message ->
            message.role == ChatMessageRole.ASSISTANT &&
                !message.isComplete &&
                (message.kind == SidePanelMessageKind.TEXT ||
                    message.kind == SidePanelMessageKind.OUTPUT_STREAM)
        }
    }
}
