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

        val outputStream = currentTurn.lastOrNull { it.kind == SidePanelMessageKind.OUTPUT_STREAM }
        val assistantText = currentTurn.lastOrNull {
            it.role == ChatMessageRole.ASSISTANT && it.kind == SidePanelMessageKind.TEXT
        }

        val target = when {
            assistantText != null && assistantText.content.isNotBlank() -> assistantText
            outputStream != null -> outputStream
            assistantText != null -> assistantText
            else -> currentTurn.lastOrNull {
                it.role == ChatMessageRole.ASSISTANT && it.kind == SidePanelMessageKind.THINKING
            }
        }

        return if (target != null) {
            messages.indexOfFirst { it.id == target.id }
        } else {
            messages.lastIndex
        }
    }
}
