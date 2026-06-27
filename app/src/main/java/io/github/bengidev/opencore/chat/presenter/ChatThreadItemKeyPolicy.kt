package io.github.bengidev.opencore.chat.presenter

import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

/** Stable LazyColumn keys for chat thread rows. */
internal object ChatThreadItemKeyPolicy {
    fun keyFor(message: SidePanelMessage): String = message.threadItemKey

    fun keysFor(messages: List<SidePanelMessage>): List<String> =
        messages.map(::keyFor)

    fun hasUniqueKeys(messages: List<SidePanelMessage>): Boolean {
        val keys = keysFor(messages)
        return keys.size == keys.toSet().size
    }
}
