package io.github.bengidev.opencore.chat.presenter

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind

/** Chooses which message row the thread should scroll to above the composer inset. */
internal object ChatThreadScrollTarget {
    fun scrollIndex(messages: List<SidePanelMessage>): Int {
        if (messages.isEmpty()) return -1

        val last = messages.last()
        if (last.role == ChatMessageRole.USER) {
            return messages.lastIndex
        }

        val turnStart = messages.indexOfLast { it.role == ChatMessageRole.USER } + 1
        val currentTurn = messages.subList(turnStart, messages.size)

        for (message in currentTurn.reversed()) {
            when {
                message.role == ChatMessageRole.ASSISTANT &&
                    message.kind == SidePanelMessageKind.TEXT ->
                    return messages.indexOfFirst { it.id == message.id }
                message.kind == SidePanelMessageKind.OUTPUT_STREAM ->
                    return messages.indexOfFirst { it.id == message.id }
            }
        }

        return messages.lastIndex
    }
}
